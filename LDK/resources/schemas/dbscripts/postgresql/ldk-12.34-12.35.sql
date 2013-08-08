--correct earlier script, place in ldk
DROP FUNCTION IF EXISTS naturalize_nextunit(text);
DROP FUNCTION IF EXISTS naturalize(text);

DROP FUNCTION IF EXISTS ldk.naturalize_nextunit(text);
DROP FUNCTION IF EXISTS ldk.naturalize(text);

CREATE FUNCTION ldk.naturalize_nextunit(text) RETURNS text AS $$
SELECT
  CASE WHEN $1 ~ '^[^0-9]+' THEN COALESCE( SUBSTR( $1, LENGTH(SUBSTRING($1 FROM '[^0-9]+'))+1 ), '' )
ELSE COALESCE( SUBSTR( $1, LENGTH(SUBSTRING($1 FROM '[0-9]+'))+1 ), '' )
END
$$ LANGUAGE SQL;

CREATE FUNCTION ldk.naturalize(text) RETURNS text AS $$
SELECT
  CASE WHEN char_length($1)>0 THEN
    CASE
    WHEN $1 ~ '^[^0-9]+' THEN SUBSTR(COALESCE(SUBSTRING($1 FROM '^[^0-9]+'), ''), 1, 25) || ldk.naturalize(ldk.naturalize_nextunit($1))
ELSE LPAD(SUBSTR(COALESCE(SUBSTRING($1 FROM '^[0-9]+'), ''), 1, 25), 25, '0') || ldk.naturalize(ldk.naturalize_nextunit($1))
END
ELSE $1
END
;
$$ LANGUAGE SQL;