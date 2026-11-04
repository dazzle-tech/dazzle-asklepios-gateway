-- Shared helpers for Admin Amendment trigger audit → encounter_amendment_change_log.
-- Session binding: prefer SET LOCAL app.reopen_session_id; else resolve OPEN session for the encounter.

CREATE OR REPLACE FUNCTION public.fn_amendment_resolve_session(p_encounter_id bigint)
RETURNS bigint
LANGUAGE plpgsql
STABLE
AS $$
DECLARE
  v_raw text;
  v_session_id bigint;
BEGIN
  IF p_encounter_id IS NULL THEN
    RETURN NULL;
  END IF;

  v_raw := current_setting('app.reopen_session_id', true);
  IF v_raw IS NOT NULL AND btrim(v_raw) <> '' AND v_raw ~ '^[0-9]+$' THEN
    v_session_id := v_raw::bigint;
    IF EXISTS (
      SELECT 1
      FROM public.encounter_reopen_session s
      WHERE s.id = v_session_id
        AND s.encounter_id = p_encounter_id
        AND s.status = 'OPEN'
    ) THEN
      RETURN v_session_id;
    END IF;
    -- Bound session is invalid for this encounter: do not attribute elsewhere.
    RAISE EXCEPTION
      'Amendment audit: app.reopen_session_id=% is not an OPEN session for encounter_id=%',
      v_raw, p_encounter_id
      USING ERRCODE = '22023';
  END IF;

  SELECT s.id
  INTO v_session_id
  FROM public.encounter_reopen_session s
  WHERE s.encounter_id = p_encounter_id
    AND s.status = 'OPEN'
  ORDER BY s.id
  LIMIT 1;

  RETURN v_session_id;
END;
$$;

CREATE OR REPLACE FUNCTION public.fn_amendment_snake_to_camel(p_key text)
RETURNS text
LANGUAGE plpgsql
IMMUTABLE
AS $$
DECLARE
  v_parts text[];
  v_out text;
  i int;
BEGIN
  IF p_key IS NULL OR p_key = '' THEN
    RETURN p_key;
  END IF;
  -- already camelCase
  IF position('_' in p_key) = 0 THEN
    RETURN p_key;
  END IF;
  v_parts := string_to_array(lower(p_key), '_');
  v_out := v_parts[1];
  FOR i IN 2..coalesce(array_length(v_parts, 1), 1) LOOP
    IF v_parts[i] IS NULL OR v_parts[i] = '' THEN
      CONTINUE;
    END IF;
    v_out := v_out || upper(substr(v_parts[i], 1, 1)) || substr(v_parts[i], 2);
  END LOOP;
  RETURN v_out;
END;
$$;

CREATE OR REPLACE FUNCTION public.fn_amendment_clinical_payload(p_row jsonb)
RETURNS jsonb
LANGUAGE plpgsql
IMMUTABLE
AS $$
DECLARE
  v_skip text[] := ARRAY[
    'id',
    'patient_id',
    'patientId',
    'encounter_id',
    'encounterId',
    'created_by',
    'createdBy',
    'created_date',
    'createdDate',
    'last_modified_by',
    'lastModifiedBy',
    'last_modified_date',
    'lastModifiedDate',
    'version',
    'reopen_session_id',
    'reopenSessionId',
    'prescription_num',
    'prescriptionNum',
    'consultation_number',
    'consultationNumber'
  ];
  v_key text;
  v_out jsonb := '{}'::jsonb;
  v_camel text;
BEGIN
  IF p_row IS NULL OR p_row = 'null'::jsonb THEN
    RETURN NULL;
  END IF;

  FOR v_key IN SELECT jsonb_object_keys(p_row)
  LOOP
    IF v_key = ANY (v_skip) THEN
      CONTINUE;
    END IF;
    -- skip nested object/array payloads from to_jsonb of relations if any
    IF jsonb_typeof(p_row -> v_key) IN ('object', 'array') THEN
      -- keep only plain arrays of scalars/objects already built by callers
      IF jsonb_typeof(p_row -> v_key) = 'array' THEN
        v_camel := public.fn_amendment_snake_to_camel(v_key);
        v_out := v_out || jsonb_build_object(v_camel, p_row -> v_key);
      END IF;
      CONTINUE;
    END IF;
    v_camel := public.fn_amendment_snake_to_camel(v_key);
    v_out := v_out || jsonb_build_object(v_camel, p_row -> v_key);
  END LOOP;

  IF v_out = '{}'::jsonb THEN
    RETURN NULL;
  END IF;
  RETURN v_out;
END;
$$;

CREATE OR REPLACE FUNCTION public.fn_amendment_retain_changes(p_before jsonb, p_after jsonb)
RETURNS jsonb
LANGUAGE plpgsql
IMMUTABLE
AS $$
DECLARE
  v_before jsonb := '{}'::jsonb;
  v_after jsonb := '{}'::jsonb;
  v_key text;
  v_left jsonb := coalesce(p_before, '{}'::jsonb);
  v_right jsonb := coalesce(p_after, '{}'::jsonb);
BEGIN
  FOR v_key IN SELECT jsonb_object_keys(v_left)
  LOOP
    IF (v_left -> v_key) IS DISTINCT FROM (v_right -> v_key) THEN
      v_before := v_before || jsonb_build_object(v_key, v_left -> v_key);
      v_after := v_after || jsonb_build_object(v_key, v_right -> v_key);
    END IF;
  END LOOP;

  FOR v_key IN SELECT jsonb_object_keys(v_right)
  LOOP
    IF NOT (v_left ? v_key) AND (v_right -> v_key) IS NOT NULL AND jsonb_typeof(v_right -> v_key) <> 'null' THEN
      v_before := v_before || jsonb_build_object(v_key, NULL);
      v_after := v_after || jsonb_build_object(v_key, v_right -> v_key);
    END IF;
  END LOOP;

  IF v_before = '{}'::jsonb AND v_after = '{}'::jsonb THEN
    RETURN NULL;
  END IF;

  RETURN jsonb_build_object('before', v_before, 'after', v_after);
END;
$$;

CREATE OR REPLACE FUNCTION public.fn_amendment_is_cancellation(p_before jsonb, p_after jsonb)
RETURNS boolean
LANGUAGE plpgsql
IMMUTABLE
AS $$
DECLARE
  v_old_status text;
  v_new_status text;
  v_old_cancelled boolean;
  v_new_cancelled boolean;
BEGIN
  v_old_status := upper(coalesce(p_before ->> 'status', ''));
  v_new_status := upper(coalesce(p_after ->> 'status', ''));
  IF v_new_status IN ('CANCELLED', 'CANCELED') AND v_old_status IS DISTINCT FROM v_new_status THEN
    RETURN true;
  END IF;

  v_old_cancelled := coalesce((p_before ->> 'isCancelled')::boolean, (p_before ->> 'is_cancelled')::boolean, false);
  v_new_cancelled := coalesce((p_after ->> 'isCancelled')::boolean, (p_after ->> 'is_cancelled')::boolean, false);
  IF v_new_cancelled AND NOT v_old_cancelled THEN
    RETURN true;
  END IF;

  IF (p_after ? 'cancelledBy' OR p_after ? 'cancelled_by' OR p_after ? 'cancelledAt' OR p_after ? 'cancelled_at'
      OR p_after ? 'cancelledDate' OR p_after ? 'cancelled_date')
     AND (
       (p_before ->> 'cancelledBy') IS DISTINCT FROM (p_after ->> 'cancelledBy')
       OR (p_before ->> 'cancelled_by') IS DISTINCT FROM (p_after ->> 'cancelled_by')
       OR (p_before ->> 'cancelledAt') IS DISTINCT FROM (p_after ->> 'cancelledAt')
       OR (p_before ->> 'cancelled_at') IS DISTINCT FROM (p_after ->> 'cancelled_at')
       OR (p_before ->> 'cancelledDate') IS DISTINCT FROM (p_after ->> 'cancelledDate')
       OR (p_before ->> 'cancelled_date') IS DISTINCT FROM (p_after ->> 'cancelled_date')
     )
     AND (
       (p_after ->> 'cancelledBy') IS NOT NULL
       OR (p_after ->> 'cancelled_by') IS NOT NULL
       OR (p_after ->> 'cancelledAt') IS NOT NULL
       OR (p_after ->> 'cancelled_at') IS NOT NULL
       OR (p_after ->> 'cancelledDate') IS NOT NULL
       OR (p_after ->> 'cancelled_date') IS NOT NULL
     )
  THEN
    RETURN true;
  END IF;

  RETURN false;
END;
$$;

CREATE OR REPLACE FUNCTION public.fn_amendment_is_pure_deactivation(p_before jsonb, p_after jsonb)
RETURNS boolean
LANGUAGE plpgsql
IMMUTABLE
AS $$
DECLARE
  v_diff jsonb;
  v_keys text[];
BEGIN
  IF coalesce((p_before ->> 'isActive')::boolean, (p_before ->> 'is_active')::boolean, true) IS NOT TRUE THEN
    RETURN false;
  END IF;
  IF coalesce((p_after ->> 'isActive')::boolean, (p_after ->> 'is_active')::boolean, true) IS NOT FALSE THEN
    RETURN false;
  END IF;

  v_diff := public.fn_amendment_retain_changes(p_before, p_after);
  IF v_diff IS NULL THEN
    RETURN false;
  END IF;
  SELECT array_agg(k ORDER BY k)
  INTO v_keys
  FROM jsonb_object_keys(v_diff -> 'after') AS k;

  RETURN v_keys = ARRAY['isActive'] OR v_keys = ARRAY['is_active'];
END;
$$;

CREATE OR REPLACE FUNCTION public.fn_amendment_write_change(
  p_encounter_id bigint,
  p_medical_sheet text,
  p_action text,
  p_record_id bigint,
  p_before jsonb,
  p_after jsonb,
  p_changed_by text,
  p_preserve_full_snapshot boolean
)
RETURNS void
LANGUAGE plpgsql
AS $$
DECLARE
  v_session_id bigint;
  v_before jsonb := p_before;
  v_after jsonb := p_after;
  v_diff jsonb;
  v_payload jsonb;
BEGIN
  IF p_encounter_id IS NULL OR p_medical_sheet IS NULL OR p_action IS NULL THEN
    RETURN;
  END IF;

  v_session_id := public.fn_amendment_resolve_session(p_encounter_id);
  IF v_session_id IS NULL THEN
    -- No OPEN Admin Amendment: ordinary clinical write, do not create amendment history.
    RETURN;
  END IF;

  IF p_action IN ('CHANGED', 'CANCELLED') AND NOT coalesce(p_preserve_full_snapshot, false) THEN
    v_diff := public.fn_amendment_retain_changes(v_before, v_after);
    IF v_diff IS NULL THEN
      RETURN;
    END IF;
    v_before := v_diff -> 'before';
    v_after := v_diff -> 'after';
  ELSIF p_action = 'ADDED' THEN
    IF v_after IS NULL OR v_after = '{}'::jsonb THEN
      RETURN;
    END IF;
    v_before := NULL;
  ELSIF p_action = 'REMOVED' THEN
    IF v_before IS NULL OR v_before = '{}'::jsonb THEN
      RETURN;
    END IF;
    v_after := NULL;
  END IF;

  v_payload := jsonb_build_object(
    'medicalSheet', p_medical_sheet,
    'action', p_action,
    'recordId', p_record_id,
    'before', v_before,
    'after', v_after
  );

  INSERT INTO public.encounter_amendment_change_log (
    reopen_session_id,
    encounter_id,
    change_data,
    changed_by,
    changed_at
  ) VALUES (
    v_session_id,
    p_encounter_id,
    v_payload,
    p_changed_by,
    now()
  );
END;
$$;

CREATE OR REPLACE FUNCTION public.fn_amendment_write_change(
  p_encounter_id bigint,
  p_medical_sheet text,
  p_action text,
  p_record_id bigint,
  p_before jsonb,
  p_after jsonb,
  p_changed_by text
)
RETURNS void
LANGUAGE plpgsql
AS $$
BEGIN
  PERFORM public.fn_amendment_write_change(
    p_encounter_id, p_medical_sheet, p_action, p_record_id,
    p_before, p_after, p_changed_by, false
  );
END;
$$;

CREATE OR REPLACE FUNCTION public.fn_amendment_allergy_ingredients(p_allergy_id bigint)
RETURNS jsonb
LANGUAGE sql
STABLE
AS $$
  SELECT coalesce(
    (
      SELECT jsonb_agg(
               jsonb_build_object('activeIngredientId', i.active_ingredient_id)
               ORDER BY i.active_ingredient_id
             )
      FROM (
        SELECT DISTINCT active_ingredient_id
        FROM public.patient_allergies_active_ingredients
        WHERE patient_allergies_id = p_allergy_id
          AND active_ingredient_id IS NOT NULL
      ) i
    ),
    '[]'::jsonb
  );
$$;

-- Transaction-local baseline of allergy ingredients before child-row mutations.
CREATE OR REPLACE FUNCTION public.fn_amendment_allergy_baseline_ensure()
RETURNS void
LANGUAGE plpgsql
AS $$
BEGIN
  CREATE TEMP TABLE IF NOT EXISTS tmp_amendment_allergy_ingredients (
    allergy_id bigint PRIMARY KEY,
    ingredients jsonb NOT NULL
  ) ON COMMIT DROP;
END;
$$;

CREATE OR REPLACE FUNCTION public.fn_amendment_allergy_baseline_capture(p_allergy_id bigint)
RETURNS void
LANGUAGE plpgsql
AS $$
BEGIN
  IF p_allergy_id IS NULL THEN
    RETURN;
  END IF;
  PERFORM public.fn_amendment_allergy_baseline_ensure();
  INSERT INTO tmp_amendment_allergy_ingredients (allergy_id, ingredients)
  VALUES (p_allergy_id, public.fn_amendment_allergy_ingredients(p_allergy_id))
  ON CONFLICT (allergy_id) DO NOTHING;
END;
$$;

CREATE OR REPLACE FUNCTION public.fn_amendment_allergy_baseline_get(p_allergy_id bigint)
RETURNS jsonb
LANGUAGE plpgsql
AS $$
DECLARE
  v_ingredients jsonb;
BEGIN
  PERFORM public.fn_amendment_allergy_baseline_ensure();
  SELECT ingredients INTO v_ingredients
  FROM tmp_amendment_allergy_ingredients
  WHERE allergy_id = p_allergy_id;
  RETURN v_ingredients;
END;
$$;
