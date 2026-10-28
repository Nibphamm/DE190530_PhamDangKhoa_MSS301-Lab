USE [MSS301_2026_PE_MEDIA]
GO
INSERT INTO users (username,email,role,rate_per_post,max_daily_posts,level,status) VALUES
('creator_a','a@example.com','CREATOR',500000,3,1,'ACTIVE'),
('creator_b','b@example.com','CREATOR',300000,1,1,'ACTIVE'),
('creator_c','c@example.com','CREATOR',800000,4,2,'SUSPENDED');
GO
