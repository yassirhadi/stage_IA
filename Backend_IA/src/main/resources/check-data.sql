-- فحص عدد التوصيات في قاعدة البيانات
SELECT COUNT(*) as total_recommendations FROM recommendations;

-- فحص التوصيات وحالاتها
SELECT status, COUNT(*) as count FROM recommendations GROUP BY status;

-- فحص عدد المخاطر
SELECT COUNT(*) as total_risks FROM risks;

-- فحص المستندات و security_score
SELECT COUNT(*) as total_documents, 
       AVG(security_score) as avg_score,
       MIN(security_score) as min_score,
       MAX(security_score) as max_score 
FROM analysis_results;

-- فحص المستخدمين
SELECT COUNT(*) as total_users FROM users;

-- فحص الأدوار
SELECT COUNT(*) as total_roles FROM roles;

-- فحص التوصيات مع تفاصيلها
SELECT r.id, r.status, r.priority, r.description, 
       risk.title as risk_title, risk.severity
FROM recommendations r
LEFT JOIN risks risk ON r.risk_id = risk.id
LIMIT 10;

-- فحص المخاطر مع تفاصيلها
SELECT id, title, severity, status, document_id FROM risks LIMIT 10;
