-- Populate SVG seat map coordinates for all existing sections.
-- Layout: stacked horizontal bars on a 600x400 canvas.
-- Stage area occupies y=5..35; sections fill y=45..395.

WITH section_layout AS (
    SELECT
        s.id,
        s.name,
        s.sort_order,
        COUNT(*) OVER (PARTITION BY s.venue_id) AS section_count,
        ROW_NUMBER() OVER (PARTITION BY s.venue_id ORDER BY s.sort_order) - 1 AS section_index
    FROM sections s
)
UPDATE sections
SET
    svg_path_id  = LOWER(REPLACE(sl.name, ' ', '-')),
    svg_x        = 50,
    svg_y        = ROUND(45 + sl.section_index * ((350.0 - (sl.section_count - 1) * 8) / sl.section_count + 8)),
    svg_width    = 500,
    svg_height   = ROUND((350.0 - (sl.section_count - 1) * 8) / sl.section_count)
FROM section_layout sl
WHERE sections.id = sl.id;
