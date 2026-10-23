/*
# Referral Tracking Table

1. New Tables
- `referrals` - Tracks referral relationships between users
  - `id` (uuid, primary key)
  - `referrer_id` (uuid, references profiles.id) - the person who shared their referral link
  - `referred_id` (uuid, references profiles.id) - the new user who signed up
  - `referral_code` (text) - the code used during signup
  - `reward_credits` (integer, default 50) - credits awarded to referrer
  - `status` (text, default 'completed') - status of the referral
  - `created_at` (timestamptz)

2. Security
- Enable RLS on `referrals`
- Grant SELECT, INSERT to `authenticated`
- Users can read referrals where they are the referrer or the referred user
- Users can insert referrals for themselves (as referred) 
- A trigger awards credits to the referrer upon referral insertion

3. Important Notes
- The referral system works as follows: when a new user signs up with a ?ref=username param,
  the frontend finds the referrer's profile and inserts a row here.
- A trigger function `award_referral_credits()` adds 50 credits to the referrer's ai_credits balance
  and sends a notification when a referral row is inserted.
*/

CREATE TABLE IF NOT EXISTS public.referrals (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  referrer_id uuid NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
  referred_id uuid NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
  referral_code text,
  reward_credits integer NOT NULL DEFAULT 50,
  status text NOT NULL DEFAULT 'completed',
  created_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE(referred_id)
);

ALTER TABLE public.referrals ENABLE ROW LEVEL SECURITY;

GRANT SELECT, INSERT ON public.referrals TO authenticated;

DROP POLICY IF EXISTS "select_own_referrals" ON public.referrals;
CREATE POLICY "select_own_referrals"
  ON public.referrals FOR SELECT
  TO authenticated
  USING (auth.uid() = referrer_id OR auth.uid() = referred_id);

DROP POLICY IF EXISTS "insert_referral" ON public.referrals;
CREATE POLICY "insert_referral"
  ON public.referrals FOR INSERT
  TO authenticated
  WITH CHECK (auth.uid() = referred_id);

-- Function to award credits to referrer when a referral is created
CREATE OR REPLACE FUNCTION public.award_referral_credits()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
AS $function$
BEGIN
  -- Add reward credits to referrer's ai_credits balance
  UPDATE public.ai_credits
  SET balance = balance + NEW.reward_credits,
      free_credits = free_credits + NEW.reward_credits,
      updated_at = now()
  WHERE user_id = NEW.referrer_id;

  -- Notify the referrer
  INSERT INTO public.notifications (user_id, title, message, notification_type)
  SELECT
    NEW.referrer_id,
    '🎁 Crédits de parrainage reçus !',
    'Félicitations ! Un nouvel utilisateur a rejoint PANU grâce à vous. Vous avez reçu +' || NEW.reward_credits || ' crédits IA.',
    'referral_reward'
  WHERE EXISTS (SELECT 1 FROM public.ai_credits WHERE user_id = NEW.referrer_id);

  RETURN NEW;
END;
$function$;

DROP TRIGGER IF EXISTS trg_award_referral_credits ON public.referrals;
CREATE TRIGGER trg_award_referral_credits
  AFTER INSERT ON public.referrals
  FOR EACH ROW
  EXECUTE FUNCTION public.award_referral_credits();

REVOKE EXECUTE ON FUNCTION public.award_referral_credits() FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.award_referral_credits() TO authenticated;
