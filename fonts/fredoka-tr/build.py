"""Build Fredoka Medium and SemiBold with Ğ ğ İ Ş ş added as composite glyphs, from recipe.json.

Usage, normally through build.sh:
    python3 -I build.py [--record] [--out-dir DIR]

Everything comes from recipe.json beside this file: the inputs (refused if a sha256 differs), the
five glyphs, each weight's (x, y) mark offsets and the name strings. No offset is derived here; the
recipe is the record (README.md, Placement). Each weight is written to the recipe's output path
under the repository root, or into --out-dir. --record writes the outputs' sha256 and sizes and the
tool versions back into recipe.json; without it the recipe is only read.

Three rules keep every original byte of the upstream font in place (docs/tech-design.md §11.1, Q-039):
- the font that is written is opened with recalcTimestamp=False and recalcBBoxes=False, so
  head.modified and every stored bounding box stay the upstream's;
- every measurement is read from a second, throwaway copy (the probe), so the written font never
  expands an original glyph and each one's glyf bytes are copied through as they are;
- maxp's composite limits are raised by hand from the probe's counts; nothing else in maxp moves.
"""
import argparse
import copy
import hashlib
import importlib.metadata
import json
import platform
import re
import sys
from pathlib import Path

import fontTools
from fontTools.pens.recordingPen import DecomposingRecordingPen
from fontTools.ttLib import TTFont
from fontTools.ttLib.tables._g_l_y_f import Glyph, GlyphComponent

HERE = Path(__file__).resolve().parent
REPOSITORY = HERE.parent.parent
RECIPE = HERE / "recipe.json"

# The component flags of the font's own two-part composites (Ccedilla, Scaron, Odieresis):
# ROUND_XY_TO_GRID | USE_MY_METRICS | OVERLAP_COMPOUND on the base letter, ROUND_XY_TO_GRID on the mark.
BASE_FLAGS = 0x0604
MARK_FLAGS = 0x0004
PAIR_POSITIONING = 2
EXTENSION = 9
VARIATION_SEQUENCES = 14


def fail(message):
    print(f"build.py: {message}", file=sys.stderr)
    sys.exit(2)


def sha256(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def code_point(written):
    return int(written.removeprefix("U+"), 16)


def check_inputs(recipe):
    for item in recipe["inputs"]:
        actual = sha256(HERE / item["file"])
        if actual != item["sha256"]:
            fail(f"{item['file']}: sha256 {actual}, the recipe pins {item['sha256']}; refusing to build")


def outline_points(glyph_set, name):
    pen = DecomposingRecordingPen(glyph_set)
    glyph_set[name].draw(pen)
    return [point for _, args in pen.value for point in args if point is not None]


def bounds(glyph_set, name):
    points = outline_points(glyph_set, name)
    xs, ys = [x for x, _ in points], [y for _, y in points]
    return min(xs), min(ys), max(xs), max(ys)


def component_depth(glyf, name):
    glyph = glyf[name]
    if not glyph.isComposite():
        return 0
    return 1 + max(component_depth(glyf, part.glyphName) for part in glyph.components)


def pair_subtables(gpos):
    """Every pair-positioning subtable, those wrapped in Extension lookups included."""
    for lookup in gpos.LookupList.Lookup:
        for subtable in lookup.SubTable:
            if lookup.LookupType == PAIR_POSITIONING:
                yield subtable
            elif lookup.LookupType == EXTENSION and subtable.ExtensionLookupType == PAIR_POSITIONING:
                yield subtable.ExtSubTable


def copy_kerning(font, plan):
    """Every pair-kerning entry and class of each base letter, given to its composite as well."""
    copied = 0
    for subtable in pair_subtables(font["GPOS"].table):
        if subtable.Format == 1:
            coverage = subtable.Coverage.glyphs
            for name, base in plan:
                if base in coverage:
                    subtable.PairSet.append(copy.deepcopy(subtable.PairSet[coverage.index(base)]))
                    coverage.append(name)
                    copied += 1
            for pair_set in subtable.PairSet:
                added = []
                for record in pair_set.PairValueRecord:
                    for name, base in plan:
                        if record.SecondGlyph == base:
                            twin = copy.deepcopy(record)
                            twin.SecondGlyph = name
                            added.append(twin)
                if added:
                    pair_set.PairValueRecord.extend(added)
                    pair_set.PairValueRecord.sort(key=lambda record: font.getGlyphID(record.SecondGlyph))
                    pair_set.PairValueCount = len(pair_set.PairValueRecord)
                    copied += len(added)
            subtable.PairSetCount = len(subtable.PairSet)
        elif subtable.Format == 2:
            coverage = subtable.Coverage.glyphs
            first_classes, second_classes = subtable.ClassDef1.classDefs, subtable.ClassDef2.classDefs
            for name, base in plan:
                if base in coverage:
                    coverage.append(name)
                    if base in first_classes:
                        first_classes[name] = first_classes[base]
                    copied += 1
                if base in second_classes:
                    second_classes[name] = second_classes[base]
                    copied += 1
    return copied


def build_weight(recipe, weight, out_path):
    source = HERE / weight["input"]
    probe = TTFont(source)  # measurements only; never written
    font = TTFont(source, recalcTimestamp=False, recalcBBoxes=False)
    probe_glyf, probe_glyphs = probe["glyf"], probe.getGlyphSet()
    glyf, hmtx, maxp = font["glyf"], font["hmtx"], font["maxp"]

    order = font.getGlyphOrder()
    for glyph in recipe["glyphs"]:
        if glyph["name"] in order:
            fail(f"{source.name} already has a glyph {glyph['name']}")
        for part in (glyph["base"], glyph["mark"]):
            if part not in order:
                fail(f"{source.name} has no glyph {part}")
    font.setGlyphOrder(order + [glyph["name"] for glyph in recipe["glyphs"]])

    for glyph in recipe["glyphs"]:
        name, base, mark = glyph["name"], glyph["base"], glyph["mark"]
        dx, dy = weight["offsets"][name]
        first, second = GlyphComponent(), GlyphComponent()
        first.glyphName, first.x, first.y, first.flags = base, 0, 0, BASE_FLAGS
        second.glyphName, second.x, second.y, second.flags = mark, dx, dy, MARK_FLAGS
        composite = Glyph()
        composite.numberOfContours = -1
        composite.components = [first, second]
        base_box, mark_box = bounds(probe_glyphs, base), bounds(probe_glyphs, mark)
        composite.xMin = min(base_box[0], mark_box[0] + dx)
        composite.yMin = min(base_box[1], mark_box[1] + dy)
        composite.xMax = max(base_box[2], mark_box[2] + dx)
        composite.yMax = max(base_box[3], mark_box[3] + dy)
        glyf[name] = composite
        hmtx.metrics[name] = (hmtx.metrics[base][0], composite.xMin)
        for table in font["cmap"].tables:
            if table.isUnicode() and table.format != VARIATION_SEQUENCES:
                table.cmap[code_point(glyph["codepoint"])] = name

        points = sum(len(probe_glyf[part].getCoordinates(probe_glyf)[0]) for part in (base, mark))
        contours = sum(len(probe_glyf[part].getCoordinates(probe_glyf)[1]) for part in (base, mark))
        maxp.maxCompositePoints = max(maxp.maxCompositePoints, points)
        maxp.maxCompositeContours = max(maxp.maxCompositeContours, contours)
        maxp.maxComponentElements = max(maxp.maxComponentElements, len(composite.components))
        depth = 1 + max(component_depth(probe_glyf, base), component_depth(probe_glyf, mark))
        maxp.maxComponentDepth = max(maxp.maxComponentDepth, depth)
        print(
            f"  {name:11} {glyph['codepoint']} = {base} + {mark} at ({dx}, {dy}); advance {hmtx.metrics[base][0]}; "
            f"box ({composite.xMin}, {composite.yMin}, {composite.xMax}, {composite.yMax}); {points} points"
        )

    glyph_classes = font["GDEF"].table.GlyphClassDef
    if glyph_classes:
        for glyph in recipe["glyphs"]:
            glyph_classes.classDefs[glyph["name"]] = glyph_classes.classDefs.get(glyph["base"], 1)

    plan = [(glyph["name"], glyph["base"]) for glyph in recipe["glyphs"]]
    print(f"  kerning entries copied: {copy_kerning(font, plan)}")

    for record in font["name"].names:
        suffix = recipe["name_suffixes"].get(str(record.nameID))
        if suffix:
            record.string = record.toUnicode() + suffix

    out_path.parent.mkdir(parents=True, exist_ok=True)
    font.save(out_path)


def write_recipe(recipe):
    text = json.dumps(recipe, indent=2, ensure_ascii=False)
    text = re.sub(r"\[\s+(-?\d+),\s+(-?\d+)\s+\]", r"[\1, \2]", text)  # an (x, y) pair on one line
    RECIPE.write_text(text + "\n", encoding="utf-8")


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--record", action="store_true", help="write the outputs' sha256 and sizes into recipe.json")
    parser.add_argument("--out-dir", type=Path, help="write the fonts here instead of the recipe's paths")
    args = parser.parse_args()
    if args.record and args.out_dir:
        fail("--record writes the recipe of the shipped files; it cannot be combined with --out-dir")

    recipe = json.loads(RECIPE.read_text(encoding="utf-8"))
    check_inputs(recipe)
    for weight in recipe["weights"]:
        out_path = (args.out_dir / Path(weight["output"]).name) if args.out_dir else REPOSITORY / weight["output"]
        print(f"{weight['weight']}: {weight['input']} -> {out_path}")
        build_weight(recipe, weight, out_path)
        digest, size = sha256(out_path), out_path.stat().st_size
        print(f"  sha256 {digest}  {size} B")
        if args.record:
            weight["output_sha256"], weight["output_bytes"] = digest, size
    if args.record:
        recipe["produced_with"] = {
            "python": platform.python_version(),
            "fonttools": fontTools.version,
            "opentype-sanitizer": importlib.metadata.version("opentype-sanitizer"),
        }
        write_recipe(recipe)
        print(f"recorded the outputs in {RECIPE.relative_to(REPOSITORY)}")


if __name__ == "__main__":
    main()
