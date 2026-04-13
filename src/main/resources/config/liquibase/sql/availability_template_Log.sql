-- config/liquibase/sql/availability_template_Log.sql

CREATE OR REPLACE FUNCTION public.trg_availability_template_log_fn()
RETURNS trigger AS $$
BEGIN
  -- INSERT: log full row snapshot.
  IF TG_OP = 'INSERT' THEN
    INSERT INTO public.availability_template_log (
      template_id,
      operation_type,
      log_date,
      log_by,
      facility_id,
      department_id,
      template_name,
      template_type,
      resource_id,
      template_color,
      status,
      version_no,
      copy_from_template_id,
      parent_template_id,
      duration_minutes,
      default_buffer_before_minutes,
      default_buffer_after_minutes,
      parallel_capacity_value,
      default_service_id,
      number_of_resources_expected,
      require_practitioner,
      default_practitioner_id,
      require_billing,
      require_pre_assessment,
      allow_patient_portal_booking,
      require_confirmation,
      financial_details,
      working_days,
      is_active,
      created_by,
      created_date,
      last_modified_by,
      last_modified_date
    ) VALUES (
      NEW.id,
      'INSERT',
      now(),
      COALESCE(NEW.last_modified_by, NEW.created_by),
      NEW.facility_id,
      NEW.department_id,
      NEW.template_name,
      NEW.template_type,
      NEW.resource_id,
      NEW.template_color,
      NEW.status,
      NEW.version_no,
      NEW.copy_from_template_id,
      NEW.parent_template_id,
      NEW.duration_minutes,
      NEW.default_buffer_before_minutes,
      NEW.default_buffer_after_minutes,
      NEW.parallel_capacity_value,
      NEW.default_service_id,
      NEW.number_of_resources_expected,
      NEW.require_practitioner,
      NEW.default_practitioner_id,
      NEW.require_billing,
      NEW.require_pre_assessment,
      NEW.allow_patient_portal_booking,
      NEW.require_confirmation,
      NEW.financial_details,
      NEW.working_days,
      NEW.is_active,
      NEW.created_by,
      NEW.created_date,
      NEW.last_modified_by,
      NEW.last_modified_date
    );

    RETURN NEW;
  END IF;

  -- UPDATE: log full row snapshot only when any field changed.
  IF TG_OP = 'UPDATE' THEN
    IF ROW(OLD.*) IS DISTINCT FROM ROW(NEW.*) THEN
      INSERT INTO public.availability_template_log (
        template_id,
        operation_type,
        log_date,
        log_by,
        facility_id,
        department_id,
        template_name,
        template_type,
        resource_id,
        template_color,
        status,
        version_no,
        copy_from_template_id,
        parent_template_id,
        duration_minutes,
        default_buffer_before_minutes,
        default_buffer_after_minutes,
        parallel_capacity_value,
        default_service_id,
        number_of_resources_expected,
        require_practitioner,
        default_practitioner_id,
        require_billing,
        require_pre_assessment,
        allow_patient_portal_booking,
        require_confirmation,
        financial_details,
        working_days,
        is_active,
        created_by,
        created_date,
        last_modified_by,
        last_modified_date
      ) VALUES (
        NEW.id,
        'UPDATE',
        now(),
        COALESCE(NEW.last_modified_by, NEW.created_by),
        NEW.facility_id,
        NEW.department_id,
        NEW.template_name,
        NEW.template_type,
        NEW.resource_id,
        NEW.template_color,
        NEW.status,
        NEW.version_no,
        NEW.copy_from_template_id,
        NEW.parent_template_id,
        NEW.duration_minutes,
        NEW.default_buffer_before_minutes,
        NEW.default_buffer_after_minutes,
        NEW.parallel_capacity_value,
        NEW.default_service_id,
        NEW.number_of_resources_expected,
        NEW.require_practitioner,
        NEW.default_practitioner_id,
        NEW.require_billing,
        NEW.require_pre_assessment,
        NEW.allow_patient_portal_booking,
        NEW.require_confirmation,
        NEW.financial_details,
        NEW.working_days,
        NEW.is_active,
        NEW.created_by,
        NEW.created_date,
        NEW.last_modified_by,
        NEW.last_modified_date
      );
    END IF;

    RETURN NEW;
  END IF;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_availability_template_log ON public.availability_template;

CREATE TRIGGER trg_availability_template_log
  AFTER INSERT OR UPDATE
  ON public.availability_template
  FOR EACH ROW
  EXECUTE FUNCTION public.trg_availability_template_log_fn();
