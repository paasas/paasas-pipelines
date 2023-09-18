alter table cloud_run_test_report 
	add column test_branch varchar(255), 
	add column test_commit varchar(255), 
	add column test_commit_author varchar(255), 
	add column test_path varchar(255), 
	add column test_repository varchar(255), 
	add column test_tag varchar(255);