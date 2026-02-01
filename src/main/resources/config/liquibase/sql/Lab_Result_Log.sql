--changeset hanan253yahya:trg_result_log_fn_v2 splitStatements:false
CREATE OR REPLACE FUNCTION trg_result_log_fn()
RETURNS trigger AS $$
BEGIN
  IF TG_OP = 'INSERT' THEN
    IF NEW.result_value_number IS NOT NULL
       OR (NEW.result_value_text IS NOT NULL AND btrim(NEW.result_value_text) <> '') THEN
      INSERT INTO public.lab_result_log (
        result_id,
        result_date,
        result_by,
        result_value
      )
      VALUES (
        NEW.id,
        now(),
        COALESCE(NEW.last_modified_by, NEW.created_by),
        COALESCE(NEW.result_value_number::text, NEW.result_value_text)
      );
END IF;
RETURN NEW;

ELSIF TG_OP = 'UPDATE' THEN
    IF (OLD.result_value_number IS DISTINCT FROM NEW.result_value_number)
       OR (OLD.result_value_text IS DISTINCT FROM NEW.result_value_text) THEN
      INSERT INTO public.lab_result_log (
        result_id,
        result_date,
        result_by,
        result_value
      )
      VALUES (
        NEW.id,
        now(),
        COALESCE(NEW.last_modified_by, NEW.created_by),
        COALESCE(NEW.result_value_number::text, NEW.result_value_text)
      );
END IF;
RETURN NEW;
END IF;

RETURN NEW;
END;
$$ LANGUAGE plpgsql;
