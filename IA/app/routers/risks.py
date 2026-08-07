from fastapi import APIRouter
from app.db import fetch_risks, fetch_all_sensitive_data

router = APIRouter()

@router.get("/risks")
def get_risks():
    """Get all risks from database"""
    try:
        risks = fetch_risks()
        return {"success": True, "data": risks}
    except Exception as e:
        import logging
        logging.error(f"Error fetching risks: {e}")
        return {"success": False, "data": [], "error": str(e)}

@router.get("/sensitive-data")
def get_sensitive_data():
    """Get all sensitive data from database"""
    try:
        sensitive_data = fetch_all_sensitive_data()
        return {"success": True, "data": sensitive_data}
    except Exception as e:
        import logging
        logging.error(f"Error fetching sensitive data: {e}")
        return {"success": False, "data": [], "error": str(e)}
