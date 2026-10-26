-- Group B clinical tables → encounter_amendment_change_log (Admin Amendment only).
-- Requires amendment_audit_helpers.sql.

CREATE OR REPLACE FUNCTION public.fn_amendment_actor(p_new jsonb, p_old jsonb)
RETURNS text
LANGUAGE sql
STABLE
AS $$
  SELECT coalesce(
    p_new ->> 'last_modified_by',
    p_new ->> 'created_by',
    p_old ->> 'last_modified_by',
    p_old ->> 'created_by',
    nullif(current_setting('app.user', true), '')
  );
$$;

CREATE OR REPLACE FUNCTION public.fn_amendment_enrich_allergy(
  p_row jsonb,
  p_allergy_id bigint,
  p_ingredients jsonb
)
RETURNS jsonb
LANGUAGE plpgsql
IMMUTABLE
AS $$
DECLARE
  v_payload jsonb;
BEGIN
  v_payload := public.fn_amendment_clinical_payload(p_row);
  IF v_payload IS NULL THEN
    v_payload := '{}'::jsonb;
  END IF;
  IF p_ingredients IS NOT NULL AND p_ingredients <> '[]'::jsonb THEN
    v_payload := v_payload || jsonb_build_object('activeIngredients', p_ingredients);
  ELSIF p_ingredients = '[]'::jsonb THEN
    -- keep empty list only when caller explicitly tracked ingredients
    NULL;
  END IF;
  IF v_payload = '{}'::jsonb THEN
    RETURN NULL;
  END IF;
  RETURN v_payload;
END;
$$;

CREATE OR REPLACE FUNCTION public.fn_amendment_allergy_child_baseline()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
  v_allergy_id bigint;
BEGIN
  IF TG_OP = 'DELETE' THEN
    v_allergy_id := OLD.patient_allergies_id;
  ELSE
    v_allergy_id := NEW.patient_allergies_id;
  END IF;
  PERFORM public.fn_amendment_allergy_baseline_capture(v_allergy_id);
  RETURN coalesce(NEW, OLD);
END;
$$;

CREATE OR REPLACE FUNCTION public.fn_amendment_audit_row()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
  v_sheet text := TG_ARGV[0];
  v_encounter_id bigint;
  v_record_id bigint;
  v_before jsonb;
  v_after jsonb;
  v_action text;
  v_actor text;
  v_header_id bigint;
BEGIN
  IF TG_OP = 'INSERT' THEN
    v_record_id := NEW.id;
    IF TG_TABLE_NAME = 'patient_prescription_medications' THEN
      v_header_id := NEW.prescription_header_id;
      SELECT p.encounter_id INTO v_encounter_id
      FROM public.patient_prescriptions p
      WHERE p.id = v_header_id;
    ELSE
      v_encounter_id := NEW.encounter_id;
    END IF;

    IF v_sheet = 'ALLERGIES' THEN
      v_after := public.fn_amendment_enrich_allergy(
        to_jsonb(NEW), NEW.id, public.fn_amendment_allergy_ingredients(NEW.id)
      );
    ELSE
      v_after := public.fn_amendment_clinical_payload(to_jsonb(NEW));
    END IF;

    PERFORM public.fn_amendment_write_change(
      v_encounter_id, v_sheet, 'ADDED', v_record_id, NULL, v_after,
      public.fn_amendment_actor(to_jsonb(NEW), NULL)
    );
    RETURN NEW;

  ELSIF TG_OP = 'DELETE' THEN
    v_record_id := OLD.id;
    IF TG_TABLE_NAME = 'patient_prescription_medications' THEN
      v_header_id := OLD.prescription_header_id;
      SELECT p.encounter_id INTO v_encounter_id
      FROM public.patient_prescriptions p
      WHERE p.id = v_header_id;
    ELSE
      v_encounter_id := OLD.encounter_id;
    END IF;

    IF v_sheet = 'ALLERGIES' THEN
      v_before := public.fn_amendment_enrich_allergy(
        to_jsonb(OLD), OLD.id,
        coalesce(
          public.fn_amendment_allergy_baseline_get(OLD.id),
          public.fn_amendment_allergy_ingredients(OLD.id)
        )
      );
    ELSE
      v_before := public.fn_amendment_clinical_payload(to_jsonb(OLD));
    END IF;

    PERFORM public.fn_amendment_write_change(
      v_encounter_id, v_sheet, 'REMOVED', v_record_id, v_before, NULL,
      public.fn_amendment_actor(NULL, to_jsonb(OLD))
    );
    RETURN OLD;

  ELSIF TG_OP = 'UPDATE' THEN
    IF to_jsonb(OLD) IS NOT DISTINCT FROM to_jsonb(NEW) THEN
      RETURN NEW;
    END IF;

    v_record_id := NEW.id;
    IF TG_TABLE_NAME = 'patient_prescription_medications' THEN
      v_header_id := NEW.prescription_header_id;
      SELECT p.encounter_id INTO v_encounter_id
      FROM public.patient_prescriptions p
      WHERE p.id = v_header_id;
    ELSE
      v_encounter_id := NEW.encounter_id;
    END IF;

    IF v_sheet = 'ALLERGIES' THEN
      PERFORM public.fn_amendment_allergy_baseline_capture(OLD.id);
      v_before := public.fn_amendment_enrich_allergy(
        to_jsonb(OLD), OLD.id,
        coalesce(
          public.fn_amendment_allergy_baseline_get(OLD.id),
          public.fn_amendment_allergy_ingredients(OLD.id)
        )
      );
      v_after := public.fn_amendment_enrich_allergy(
        to_jsonb(NEW), NEW.id, public.fn_amendment_allergy_ingredients(NEW.id)
      );
    ELSE
      v_before := public.fn_amendment_clinical_payload(to_jsonb(OLD));
      v_after := public.fn_amendment_clinical_payload(to_jsonb(NEW));
    END IF;

    v_actor := public.fn_amendment_actor(to_jsonb(NEW), to_jsonb(OLD));

    IF public.fn_amendment_is_pure_deactivation(v_before, v_after) THEN
      -- Match Java deactivated(): keep full clinical snapshot on both sides + isActive flip.
      -- Skip field-diff so history can reconstruct prior clinical values for replacement pairing.
      v_before := coalesce(v_before, '{}'::jsonb);
      v_before := (v_before - 'isActive') || jsonb_build_object('isActive', true);
      v_after := (v_before - 'isActive') || jsonb_build_object('isActive', false);
      PERFORM public.fn_amendment_write_change(
        v_encounter_id, v_sheet, 'CHANGED', v_record_id, v_before, v_after, v_actor, true
      );
      RETURN NEW;
    END IF;

    IF public.fn_amendment_is_cancellation(v_before, v_after) THEN
      v_action := 'CANCELLED';
    ELSE
      v_action := 'CHANGED';
    END IF;

    PERFORM public.fn_amendment_write_change(
      v_encounter_id, v_sheet, v_action, v_record_id, v_before, v_after, v_actor
    );
    RETURN NEW;
  END IF;

  RETURN NULL;
END;
$$;

-- Child ingredient mutations after allergy INSERT: attach ingredients onto the ADDED row.
-- Updates go through the parent allergy UPDATE trigger (baseline captured BEFORE child writes).
CREATE OR REPLACE FUNCTION public.fn_amendment_allergy_ingredients_audit()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
  v_allergy_id bigint;
  v_encounter_id bigint;
  v_after_ingredients jsonb;
  v_session_id bigint;
  v_log_id bigint;
BEGIN
  IF TG_OP = 'DELETE' THEN
    v_allergy_id := OLD.patient_allergies_id;
  ELSE
    v_allergy_id := NEW.patient_allergies_id;
  END IF;

  IF v_allergy_id IS NULL THEN
    RETURN coalesce(NEW, OLD);
  END IF;

  SELECT a.encounter_id INTO v_encounter_id
  FROM public.patient_allergies a
  WHERE a.id = v_allergy_id;

  IF v_encounter_id IS NULL THEN
    RETURN coalesce(NEW, OLD);
  END IF;

  v_session_id := public.fn_amendment_resolve_session(v_encounter_id);
  IF v_session_id IS NULL THEN
    RETURN coalesce(NEW, OLD);
  END IF;

  v_after_ingredients := public.fn_amendment_allergy_ingredients(v_allergy_id);

  SELECT l.id
  INTO v_log_id
  FROM public.encounter_amendment_change_log l
  WHERE l.reopen_session_id = v_session_id
    AND l.encounter_id = v_encounter_id
    AND l.change_data->>'medicalSheet' = 'ALLERGIES'
    AND l.change_data->>'action' = 'ADDED'
    AND (l.change_data->>'recordId')::bigint = v_allergy_id
  ORDER BY l.id DESC
  LIMIT 1;

  IF v_log_id IS NOT NULL THEN
    UPDATE public.encounter_amendment_change_log
    SET change_data = jsonb_set(
      change_data,
      '{after}',
      coalesce(change_data->'after', '{}'::jsonb)
        || CASE
             WHEN v_after_ingredients IS NULL OR v_after_ingredients = '[]'::jsonb
               THEN '{}'::jsonb
             ELSE jsonb_build_object('activeIngredients', v_after_ingredients)
           END,
      true
    )
    WHERE id = v_log_id;
  END IF;

  RETURN coalesce(NEW, OLD);
END;
$$;

-- Attach / replace triggers
DO $$
DECLARE
  r record;
BEGIN
  FOR r IN
    SELECT * FROM (VALUES
      ('body_measurements', 'BODY_MEASUREMENTS', true),
      ('pain_assessment', 'PAIN_ASSESSMENT', true),
      ('additional_measurements', 'ADDITIONAL_MEASUREMENTS', true),
      ('patient_observations_complaints', 'OBSERVATIONS_COMPLAINTS', true),
      ('review_of_system', 'REVIEW_OF_SYSTEMS', true),
      ('patient_diagnoses', 'DIAGNOSIS', true),
      ('patient_prescriptions', 'PRESCRIPTION', true),
      ('patient_prescription_medications', 'PRESCRIPTION_MEDICATION', true),
      ('referral_requests', 'REFERRAL', true),
      ('encounter_vaccination', 'VACCINATION', true),
      ('dental_procedure', 'DENTAL_PROCEDURES', true),
      ('glasgow_coma_scale_assessment', 'GLASGOW', true),
      ('patient_flacc_pain_scale', 'FLACC', true),
      ('telephonic_consultation', 'TELEPHONIC_CONSULTATION', true),
      ('general_assessment', 'GENERAL_ASSESSMENT', true),
      ('patient_sick_leave', 'SICK_LEAVE', false),
      ('patient_allergies', 'ALLERGIES', false),
      ('patient_warnings', 'MEDICAL_WARNINGS', false)
    ) AS t(tbl, sheet, has_delete)
  LOOP
    EXECUTE format('DROP TRIGGER IF EXISTS trg_amendment_audit_%s ON public.%I', r.tbl, r.tbl);
    IF r.has_delete THEN
      EXECUTE format(
        'CREATE TRIGGER trg_amendment_audit_%s
           AFTER INSERT OR UPDATE OR DELETE ON public.%I
           FOR EACH ROW EXECUTE FUNCTION public.fn_amendment_audit_row(%L)',
        r.tbl, r.tbl, r.sheet
      );
    ELSE
      EXECUTE format(
        'CREATE TRIGGER trg_amendment_audit_%s
           AFTER INSERT OR UPDATE ON public.%I
           FOR EACH ROW EXECUTE FUNCTION public.fn_amendment_audit_row(%L)',
        r.tbl, r.tbl, r.sheet
      );
    END IF;
  END LOOP;
END;
$$;

DROP TRIGGER IF EXISTS trg_amendment_allergy_ingredients_baseline ON public.patient_allergies_active_ingredients;
CREATE TRIGGER trg_amendment_allergy_ingredients_baseline
  BEFORE INSERT OR UPDATE OR DELETE
  ON public.patient_allergies_active_ingredients
  FOR EACH ROW
  EXECUTE FUNCTION public.fn_amendment_allergy_child_baseline();

DROP TRIGGER IF EXISTS trg_amendment_allergy_ingredients ON public.patient_allergies_active_ingredients;
CREATE TRIGGER trg_amendment_allergy_ingredients
  AFTER INSERT OR UPDATE OR DELETE
  ON public.patient_allergies_active_ingredients
  FOR EACH ROW
  EXECUTE FUNCTION public.fn_amendment_allergy_ingredients_audit();
