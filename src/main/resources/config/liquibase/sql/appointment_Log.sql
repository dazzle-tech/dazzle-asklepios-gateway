-- config/liquibase/sql/appointment_Log.sql

CREATE OR REPLACE FUNCTION public.trg_appointment_log_fn()
RETURNS trigger AS $$
BEGIN
  -- INSERT: log full row snapshot.
  IF TG_OP = 'INSERT' THEN
    INSERT INTO public.appointment_log (
      appointment_id,
      operation_type,
      log_date,
      log_by,
      facility_id,
      department_id,
      availability_generation_batch_id,
      resource_type,
      resource_id,
      capacity_index,
      start_datetime,
      end_datetime,
      patient_id,
      default_service_id,
      default_practitioner_id,
      reason,
      booking_mode,
      status,
      service,
      service_group_id,
      deferred,
      deferred_at,
      no_show_reason,
      cancel_reason,
      cancelled_by,
      priority,
      origin_type,
      origin_name,
      note,
      follow_up_encounter_id,
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
      NEW.availability_generation_batch_id,
      NEW.resource_type,
      NEW.resource_id,
      NEW.capacity_index,
      NEW.start_datetime,
      NEW.end_datetime,
      NEW.patient_id,
      NEW.default_service_id,
      NEW.default_practitioner_id,
      NEW.reason,
      NEW.booking_mode,
      NEW.status,
      NEW.service,
      NEW.service_group_id,
      NEW.deferred,
      NEW.deferred_at,
      NEW.no_show_reason,
      NEW.cancel_reason,
      NEW.cancelled_by,
      NEW.priority,
      NEW.origin_type,
      NEW.origin_name,
      NEW.note,
      NEW.follow_up_encounter_id,
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
      INSERT INTO public.appointment_log (
        appointment_id,
        operation_type,
        log_date,
        log_by,
        facility_id,
        department_id,
        availability_generation_batch_id,
        resource_type,
        resource_id,
        capacity_index,
        start_datetime,
        end_datetime,
        patient_id,
        default_service_id,
        default_practitioner_id,
        reason,
        booking_mode,
        status,
        service,
        service_group_id,
        deferred,
        deferred_at,
        no_show_reason,
        cancel_reason,
        cancelled_by,
        priority,
        origin_type,
        origin_name,
        note,
        follow_up_encounter_id,
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
        NEW.availability_generation_batch_id,
        NEW.resource_type,
        NEW.resource_id,
        NEW.capacity_index,
        NEW.start_datetime,
        NEW.end_datetime,
        NEW.patient_id,
        NEW.default_service_id,
        NEW.default_practitioner_id,
        NEW.reason,
        NEW.booking_mode,
        NEW.status,
        NEW.service,
        NEW.service_group_id,
        NEW.deferred,
        NEW.deferred_at,
        NEW.no_show_reason,
        NEW.cancel_reason,
        NEW.cancelled_by,
        NEW.priority,
        NEW.origin_type,
        NEW.origin_name,
        NEW.note,
        NEW.follow_up_encounter_id,
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

DROP TRIGGER IF EXISTS trg_appointment_log ON public.appointment;

CREATE TRIGGER trg_appointment_log
  AFTER INSERT OR UPDATE
  ON public.appointment
  FOR EACH ROW
  EXECUTE FUNCTION public.trg_appointment_log_fn();
