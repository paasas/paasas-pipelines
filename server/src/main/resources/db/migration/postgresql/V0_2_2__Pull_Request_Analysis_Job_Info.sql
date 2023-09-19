alter table pull_request_analysis 
	add column timestamp timestamp(6),
	add column build varchar(255),
	add column job varchar(255),
	add column pipeline varchar(255),
	add column team varchar(255),
	add column url oid;