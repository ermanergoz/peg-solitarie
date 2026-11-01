"""Verify the built Fredoka files against their upstream inputs: the identity contract of
docs/tech-design.md §11.1, Q-039 (2). Every line prints PASS, FAIL or SKIP; any FAIL exits 1.

Usage, normally through build.sh:
    python3 -I verify.py [--out-dir DIR]

What it proves, per weight:
- the output's sha256 and size are the recipe's (unless --out-dir points at another build), and
  every input's sha256 is the recipe's;
- the upstream glyph order is kept, and exactly the recipe's glyphs follow it, in its order;
- every original glyph's raw glyf bytes and loca offsets are the upstream's, and so is its hmtx;
- every table the build does not edit is byte-identical; head differs in checkSumAdjustment only,
  hhea in numberOfHMetrics only, maxp in numGlyphs and the four composite limits only, name in IDs
  3 and 5 only (each the upstream string plus the recipe's suffix), post by the added names only;
- cmap gains exactly the recipe's code points, in every Unicode subtable, and changes nothing else;
- each composite is its base at (0, 0) and its mark at the recipe's offset, with the font's own
  component flags, the base's advance, lsb = xMin, the base's GDEF class, and a box inside the
  font's recorded extremes (head and hhea are deliberately not recomputed);
- every original kerning pair keeps its value, and each composite kerns exactly as its base, on
  either side of every glyph;
- OTS sanitises the file with no message but success;
- where hb-shape is on PATH: every value of the base strings.xml the upstream can draw shapes to
  the same glyph ids, advances and offsets in both, and each composite shapes to its one glyph.
"""
import argparse
import hashlib
import json
import os
import shutil
import subprocess
import sys
import tempfile
import xml.etree.ElementTree as ElementTree
from pathlib import Path

import ots
from fontTools.ttLib import TTFont

HERE = Path(__file__).resolve().parent
REPOSITORY = HERE.parent.parent
RECIPE = HERE / "recipe.json"
BASE_STRINGS = REPOSITORY / "app/src/main/res/values/strings.xml"

EDITED_TABLES = {"GDEF", "GPOS", "cmap", "glyf", "head", "hhea", "hmtx", "loca", "maxp", "name", "post"}
COMPOSITE_LIMITS = ("maxCompositePoints", "maxCompositeContours", "maxComponentElements", "maxComponentDepth")
POST_HEADER = ("formatType", "italicAngle", "underlinePosition", "underlineThickness", "isFixedPitch",
               "minMemType42", "maxMemType42", "minMemType1", "maxMemType1")
BASE_FLAGS, MARK_FLAGS = 0x0604, 0x0004
PAIR_POSITIONING, EXTENSION, VARIATION_SEQUENCES = 2, 9, 14
HEAD_CHECKSUM = slice(8, 12)  # checkSumAdjustment, the one head field a save must rewrite

failures = 0


def report(ok, what, detail=""):
    global failures
    failures += 0 if ok else 1
    print(f"{'PASS' if ok else 'FAIL'}  {what}{': ' + detail if detail and not ok else ''}")


def skip(what):
    print(f"SKIP  {what}")


def sha256(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def code_point(written):
    return int(written.removeprefix("U+"), 16)


def raw_glyphs(font):
    data, locations = font.getTableData("glyf"), font["loca"].locations
    return [data[locations[i]:locations[i + 1]] for i in range(len(font.getGlyphOrder()))], locations


def table_fields(table, ignored):
    return {key: value for key, value in vars(table).items() if key not in ignored}


def name_records(font):
    return {(r.nameID, r.platformID, r.platEncID, r.langID): r.toUnicode() for r in font["name"].names}


def pair_subtables(font):
    for lookup in font["GPOS"].table.LookupList.Lookup:
        for subtable in lookup.SubTable:
            if lookup.LookupType == PAIR_POSITIONING:
                yield subtable
            elif lookup.LookupType == EXTENSION and subtable.ExtensionLookupType == PAIR_POSITIONING:
                yield subtable.ExtSubTable


def value_of(record):
    return tuple(sorted(vars(record).items())) if record is not None else None


def pair_reader(subtable):
    """A function (first, second) -> the subtable's values for that pair, or None if it has none."""
    coverage = set(subtable.Coverage.glyphs)
    if subtable.Format == 1:
        rows = {
            first: {r.SecondGlyph: (value_of(r.Value1), value_of(r.Value2)) for r in pair_set.PairValueRecord}
            for first, pair_set in zip(subtable.Coverage.glyphs, subtable.PairSet)
        }
        return lambda first, second: rows[first].get(second) if first in coverage else None
    first_classes, second_classes = subtable.ClassDef1.classDefs, subtable.ClassDef2.classDefs

    def read(first, second):
        if first not in coverage:
            return None
        row = subtable.Class1Record[first_classes.get(first, 0)].Class2Record[second_classes.get(second, 0)]
        return value_of(row.Value1), value_of(row.Value2)

    return read


def unescaped(value):
    out, index = [], 0
    while index < len(value):
        char = value[index]
        if char == "\\" and index + 1 < len(value):
            following = value[index + 1]
            if following == "u":
                out.append(chr(int(value[index + 2:index + 6], 16)))
                index += 6
                continue
            out.append({"n": "\n", "t": "\t"}.get(following, following))
            index += 2
            continue
        if char != '"':
            out.append(char)
        index += 1
    return "".join(out)


def english_values():
    root = ElementTree.parse(BASE_STRINGS).getroot()
    values = [unescaped("".join(e.itertext())) for e in root if e.tag == "string"]
    values += [unescaped("".join(i.itertext())) for e in root if e.tag == "plurals" for i in e if i.tag == "item"]
    return values


def shaped(hb_shape, font_path, lines):
    with tempfile.NamedTemporaryFile("w", encoding="utf-8", suffix=".txt", delete=False) as text_file:
        text_file.write("\n".join(lines) + "\n")
    try:
        result = subprocess.run(
            [hb_shape, "--no-glyph-names", "--output-format=json", f"--text-file={text_file.name}", str(font_path)],
            capture_output=True, text=True, check=True,
        )
    finally:
        os.unlink(text_file.name)
    return [json.loads(line) for line in result.stdout.splitlines() if line.strip()]


def verify_weight(recipe, weight, built_path, check_hash):
    print(f"== {weight['weight']}: {weight['input']} against {built_path}")
    upstream_path = HERE / weight["input"]
    if check_hash:
        report(sha256(built_path) == weight.get("output_sha256"), "output sha256 is the recipe's",
               f"{sha256(built_path)} vs {weight.get('output_sha256')}")
        report(built_path.stat().st_size == weight.get("output_bytes"), "output size is the recipe's",
               f"{built_path.stat().st_size} vs {weight.get('output_bytes')}")
    upstream, built = TTFont(upstream_path, lazy=False), TTFont(built_path, lazy=False)
    names = [glyph["name"] for glyph in recipe["glyphs"]]
    original = upstream.getGlyphOrder()
    built_order = built.getGlyphOrder()
    report(built_order == original + names, "upstream glyph order kept, exactly the recipe's glyphs appended",
           f"appended {built_order[len(original):]}")

    upstream_raw, upstream_loca = raw_glyphs(upstream)
    built_raw, built_loca = raw_glyphs(built)
    changed = [original[i] for i in range(len(original)) if upstream_raw[i] != built_raw[i]]
    report(not changed, "every original glyph's raw glyf bytes unchanged", f"{changed}")
    report(upstream_loca[:len(original) + 1] == built_loca[:len(original) + 1], "every original loca offset unchanged")
    moved = [g for g in original if upstream["hmtx"].metrics[g] != built["hmtx"].metrics[g]]
    report(not moved, "every original hmtx entry unchanged", f"{moved}")

    tags = sorted((set(upstream.keys()) | set(built.keys())) - {"GlyphOrder"})
    report(set(upstream.keys()) == set(built.keys()), "same set of tables")
    untouched = [t for t in tags if t not in EDITED_TABLES]
    differing = [t for t in untouched if upstream.getTableData(t) != built.getTableData(t)]
    report(not differing, f"tables byte-identical: {' '.join(untouched)}", f"differ: {differing}")
    upstream_head, built_head = bytearray(upstream.getTableData("head")), bytearray(built.getTableData("head"))
    upstream_head[HEAD_CHECKSUM] = built_head[HEAD_CHECKSUM] = b"\0\0\0\0"
    report(upstream_head == built_head, "head identical but for checkSumAdjustment")
    report(table_fields(upstream["hhea"], {"numberOfHMetrics"}) == table_fields(built["hhea"], {"numberOfHMetrics"}),
           "hhea identical but for numberOfHMetrics")
    maxp_ignored = {"numGlyphs", *COMPOSITE_LIMITS}
    report(table_fields(upstream["maxp"], maxp_ignored) == table_fields(built["maxp"], maxp_ignored),
           "maxp identical but for numGlyphs and the four composite limits")
    report(built["maxp"].numGlyphs == len(original) + len(names), "maxp numGlyphs counts the added glyphs")
    lowered = [f for f in COMPOSITE_LIMITS if getattr(built["maxp"], f) < getattr(upstream["maxp"], f)]
    report(not lowered, "no maxp composite limit lowered", f"{lowered}")

    upstream_names, built_names = name_records(upstream), name_records(built)
    changed_ids = sorted({key[0] for key in upstream_names if upstream_names[key] != built_names.get(key)})
    report(set(upstream_names) == set(built_names) and changed_ids == sorted(int(k) for k in recipe["name_suffixes"]),
           "name differs in IDs 3 and 5 only", f"changed {changed_ids}")
    suffixed = all(built_names[key] == upstream_names[key] + recipe["name_suffixes"][str(key[0])]
                   for key in upstream_names if str(key[0]) in recipe["name_suffixes"])
    report(suffixed, "name IDs 3 and 5 are the upstream strings plus the recipe's suffixes")
    # post's names are the glyph order checked above; its header must not move.
    post_header = [getattr(upstream["post"], f) == getattr(built["post"], f) for f in POST_HEADER]
    report(all(post_header), "post header identical; its names are the glyph order above")

    expected_cmap = {code_point(glyph["codepoint"]): glyph["name"] for glyph in recipe["glyphs"]}
    unicode_tables = [t for t in upstream["cmap"].tables if t.isUnicode() and t.format != VARIATION_SEQUENCES]
    cmap_ok = True
    for table in unicode_tables:
        twin = built["cmap"].getcmap(table.platformID, table.platEncID)
        added = {k: v for k, v in twin.cmap.items() if k not in table.cmap}
        kept = all(twin.cmap.get(k) == v for k, v in table.cmap.items())
        cmap_ok &= kept and added == expected_cmap
    other_tables = [t for t in upstream["cmap"].tables if t not in unicode_tables]
    cmap_ok &= all(built["cmap"].getcmap(t.platformID, t.platEncID).cmap == t.cmap for t in other_tables
                   if hasattr(t, "cmap"))
    report(cmap_ok, f"cmap adds exactly {', '.join(g['codepoint'] for g in recipe['glyphs'])} in every Unicode subtable")

    head, hhea, built_glyf = built["head"], built["hhea"], built["glyf"]
    glyph_classes = built["GDEF"].table.GlyphClassDef.classDefs
    upstream_classes = upstream["GDEF"].table.GlyphClassDef.classDefs
    report(all(glyph_classes.get(g) == upstream_classes.get(g) for g in original), "every original GDEF class unchanged")
    for glyph in recipe["glyphs"]:
        name, base, mark = glyph["name"], glyph["base"], glyph["mark"]
        composite = built_glyf[name]
        dx, dy = weight["offsets"][name]
        parts = [(c.glyphName, c.x, c.y, c.flags) for c in composite.components]
        advance, lsb = built["hmtx"].metrics[name]
        inside = (head.xMin <= composite.xMin and head.yMin <= composite.yMin and composite.xMax <= head.xMax
                  and composite.yMax <= head.yMax and lsb >= hhea.minLeftSideBearing
                  and advance - (lsb + composite.xMax - composite.xMin) >= hhea.minRightSideBearing
                  and lsb + composite.xMax - composite.xMin <= hhea.xMaxExtent and advance <= hhea.advanceWidthMax)
        report(parts == [(base, 0, 0, BASE_FLAGS), (mark, dx, dy, MARK_FLAGS)]
               and advance == built["hmtx"].metrics[base][0] and lsb == composite.xMin
               and glyph_classes.get(name) == glyph_classes.get(base, 1) and inside,
               f"{name} = {base} + {mark} at ({dx}, {dy}), base advance {advance}, lsb {lsb}, inside the font's extremes")

    upstream_pairs = list(pair_subtables(upstream))
    built_pairs = list(pair_subtables(built))
    report(len(upstream_pairs) == len(built_pairs), "same number of pair-kerning subtables")
    twins = {glyph["name"]: glyph["base"] for glyph in recipe["glyphs"]}
    every = built_order
    kept, copied = True, True
    for upstream_subtable, built_subtable in zip(upstream_pairs, built_pairs):
        before, after = pair_reader(upstream_subtable), pair_reader(built_subtable)
        kept &= all(before(first, second) == after(first, second) for first in original for second in original)
        for name, base in twins.items():
            copied &= all(after(name, other) == after(base, twins.get(other, other)) for other in every)
            copied &= all(after(other, name) == after(twins.get(other, other), base) for other in every)
    report(kept, "every original kerning pair keeps its value")
    report(copied, "each composite kerns as its base, on either side of every glyph")

    sanitised = ots.sanitize(str(built_path), os.devnull, capture_output=True)
    messages = (sanitised.stdout + sanitised.stderr).decode("utf-8", "replace").strip()
    report(sanitised.returncode == 0 and messages in ("", "File sanitized successfully!"),
           "OTS sanitises the file with no warning", messages)

    hb_shape = shutil.which("hb-shape")
    if hb_shape is None:
        skip("hb-shape is not on PATH: the shaping parity is not run (the hashes still bind)")
        return
    drawable_cmap = upstream.getBestCmap()
    texts = [v for v in english_values() if v.strip() and "\n" not in v and all(ord(c) in drawable_cmap for c in v)]
    before, after = shaped(hb_shape, upstream_path, texts), shaped(hb_shape, built_path, texts)
    differing_texts = [t for t, a, b in zip(texts, before, after) if a != b]
    report(len(before) == len(texts) and not differing_texts,
           f"hb-shape: {len(texts)} base strings shape alike, glyph ids, advances and offsets",
           f"{len(differing_texts)} differ, e.g. {differing_texts[:3]}")
    letters = [chr(code_point(glyph["codepoint"])) for glyph in recipe["glyphs"]]
    single = shaped(hb_shape, built_path, letters)
    ids = [built.getGlyphID(glyph["name"]) for glyph in recipe["glyphs"]]
    report([[g["g"] for g in run] for run in single] == [[i] for i in ids],
           f"hb-shape: {' '.join(letters)} each shape to their one composite glyph ({ids})")


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--out-dir", type=Path, help="verify the fonts built there instead of the recipe's paths")
    args = parser.parse_args()
    recipe = json.loads(RECIPE.read_text(encoding="utf-8"))
    for item in recipe["inputs"]:
        report(sha256(HERE / item["file"]) == item["sha256"], f"input {item['file']} sha256 is the recipe's")
    for weight in recipe["weights"]:
        built = (args.out_dir / Path(weight["output"]).name) if args.out_dir else REPOSITORY / weight["output"]
        verify_weight(recipe, weight, built, check_hash=args.out_dir is None)
    print(f"verify.py: {'all checks passed' if failures == 0 else f'{failures} check(s) FAILED'}")
    sys.exit(1 if failures else 0)


if __name__ == "__main__":
    main()
