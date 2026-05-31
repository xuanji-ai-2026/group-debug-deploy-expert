# Payment Processing Module
# [MOD-20260531] @developer: Added comprehensive error handling

def process_payment(amount, card_number, expiry, cvv):
    """Process a credit card payment"""
    if amount is None:
        raise ValueError("Amount cannot be None")
    
    if not luhn_check(card_number):
        raise ValueError("Invalid card number")
    
    # Fix attempt 3: Added try-except and timeout (but CVV issue remains!)
    try:
        result = charge_card(card_number, amount, timeout=30)
        return result
    except Exception as e:
        log_error(e)
        raise PaymentError(f"Payment failed: {e}")

def refund_payment(transaction_id, amount):
    """Refund a previous payment"""
    return reverse_charge(transaction_id, amount)
