-- Fonction RPC pour déduire les crédits après une génération IA
CREATE OR REPLACE FUNCTION public.consume_ai_credits(p_user_id UUID, p_amount INTEGER)
RETURNS TABLE (new_balance INTEGER, success BOOLEAN) AS $$
DECLARE
    v_current_balance INTEGER;
BEGIN
    -- Récupérer le solde actuel
    SELECT balance INTO v_current_balance
    FROM public.ai_credits
    WHERE user_id = p_user_id;

    IF v_current_balance IS NULL THEN
        RETURN QUERY SELECT 0, false;
        RETURN;
    END IF;

    IF v_current_balance < p_amount THEN
        RETURN QUERY SELECT v_current_balance, false;
        RETURN;
    END IF;

    -- Mettre à jour le solde
    UPDATE public.ai_credits
    SET 
        balance = balance - p_amount,
        used_credits = used_credits + p_amount,
        updated_at = NOW()
    WHERE user_id = p_user_id
    RETURNING balance INTO v_current_balance;

    -- Enregistrer la transaction
    INSERT INTO public.ai_credit_transactions (user_id, amount, transaction_type, description)
    VALUES (p_user_id, -p_amount, 'usage', 'Génération IA (FAL.ai)');

    RETURN QUERY SELECT v_current_balance, true;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

GRANT EXECUTE ON FUNCTION public.consume_ai_credits(UUID, INTEGER) TO authenticated;
