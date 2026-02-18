-- config/liquibase/sql/Report_Image_Status_Log.sql

CREATE OR REPLACE FUNCTION public.trg_report_image_status_log_fn()
RETURNS trigger AS $$
BEGIN
  -- INSERT: log if image_status has a value
  IF TG_OP = 'INSERT' THEN
    IF NEW.image_status IS NOT NULL AND btrim(NEW.image_status) <> '' THEN
      INSERT INTO public.diagnostic_order_tests_report_image_status_log
        (report_id, status_date, status_by, status_value)
      VALUES
        (NEW.id, now(), COALESCE(NEW.last_modified_by, NEW.created_by), btrim(NEW.image_status));
END IF;

RETURN NEW;
END IF;

  -- UPDATE: log only if image_status changed and new value is not blank
  IF TG_OP = 'UPDATE' THEN
    IF (OLD.image_status IS DISTINCT FROM NEW.image_status) THEN
      IF NEW.image_status IS NOT NULL AND btrim(NEW.image_status) <> '' THEN
        INSERT INTO public.diagnostic_order_tests_report_image_status_log
          (report_id, status_date, status_by, status_value)
        VALUES
          (NEW.id, now(), COALESCE(NEW.last_modified_by, NEW.created_by), btrim(NEW.image_status));
END IF;
END IF;

RETURN NEW;
END IF;

RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_report_image_status_log ON public.diagnostic_order_tests_report;

CREATE TRIGGER trg_report_image_status_log
  AFTER INSERT OR UPDATE OF image_status
                  ON public.diagnostic_order_tests_report
                    FOR EACH ROW
                    EXECUTE FUNCTION public.trg_report_image_status_log_fn();
