alter table cloud_run_deployment drop column url;
alter table cloud_run_test_report drop column report_url;
alter table cloud_run_test_report drop column url;
alter table firebase_app_deployment drop column url;
alter table firebase_test_report drop column report_url;
alter table firebase_test_report drop column url;
alter table pull_request_analysis drop column url;
alter table terraform_deployment drop column url;

alter table cloud_run_deployment add column url varchar(65535);
alter table cloud_run_test_report add column report_url varchar(65535);
alter table cloud_run_test_report add column url varchar(65535);
alter table firebase_app_deployment add column url varchar(65535);
alter table firebase_test_report add column report_url varchar(65535);
alter table firebase_test_report add column url varchar(65535);
alter table pull_request_analysis add column url varchar(65535);
alter table terraform_deployment add column url varchar(65535);