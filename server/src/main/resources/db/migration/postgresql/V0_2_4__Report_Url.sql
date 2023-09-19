alter table cloud_run_test_report drop column report_url;
alter table firebase_test_report drop column report_url;

alter table cloud_run_test_report add column report_url oid;
alter table firebase_test_report add column report_url oid;