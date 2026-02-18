CREATE OR REPLACE FUNCTION trg_lab_result_log_fn()
RETURNS trigger AS $$
BEGIN
  IF TG_OP = 'INSERT' THEN
    IF NEW.result_value_number IS NOT NULL
       OR (NEW.result_value_text IS NOT NULL AND btrim(NEW.result_value_text) <> '') THEN
      INSERT INTO public.lab_result_log (
        result_id, result_date, result_by, result_value
      )
      VALUES (
        NEW.id,
        now(),
        COALESCE(NEW.last_modified_by, NEW.created_by),
        COALESCE(NEW.result_value_number::text, btrim(NEW.result_value_text))
      );
END IF;

RETURN NEW;

ELSIF TG_OP = 'UPDATE' THEN
    IF (OLD.result_value_number IS DISTINCT FROM NEW.result_value_number)
       OR (OLD.result_value_text   IS DISTINCT FROM NEW.result_value_text) THEN


      IF NEW.result_value_number IS NOT NULL
         OR (NEW.result_value_text IS NOT NULL AND btrim(NEW.result_value_text) <> '') THEN
        INSERT INTO public.lab_result_log (
          result_id, result_date, result_by, result_value
        )
        VALUES (
          NEW.id,
          now(),
          COALESCE(NEW.last_modified_by, NEW.created_by),
          COALESCE(NEW.result_value_number::text, btrim(NEW.result_value_text))
        );
END IF;

END IF;

RETURN NEW;
END IF;

RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_lab_result_log ON public.diagnostic_order_tests_result;

CREATE TRIGGER trg_lab_result_log
  AFTER INSERT OR UPDATE OF result_value_number, result_value_text
                  ON public.diagnostic_order_tests_result
                    FOR EACH ROW
                    EXECUTE FUNCTION public.trg_lab_result_log_fn();
