create table cloud_run_analysis (pull_request_analysis_number integer not null, pull_request_analysis_repository varchar(255) not null, service_name varchar(255) not null, tag varchar(255), primary key (pull_request_analysis_number, pull_request_analysis_repository, service_name));
create table cloud_run_deployment (timestamp timestamp(6), url varchar(65535), build varchar(255) not null, image varchar(255), job varchar(255) not null, pipeline varchar(255) not null, project_id varchar(255), tag varchar(255), team varchar(255) not null, app clob, primary key (build, job, pipeline, team));
create table cloud_run_test_report (successful boolean not null, timestamp timestamp(6), report_url varchar(65535), url varchar(65535), build varchar(255) not null, image varchar(255), job varchar(255) not null, pipeline varchar(255) not null, project_id varchar(255), tag varchar(255), team varchar(255) not null, test_branch varchar(255), test_commit varchar(255), test_commit_author varchar(255), test_path varchar(255), test_repository varchar(255), test_tag varchar(255), primary key (build, job, pipeline, team));
create table firebase_app_analysis (pull_request_analysis_number integer not null, pull_request_analysis_repository varchar(255) not null, primary key (pull_request_analysis_number, pull_request_analysis_repository));
create table firebase_app_deployment (timestamp timestamp(6), url varchar(65535), branch varchar(255), build varchar(255) not null, commit varchar(255), commit_author varchar(255), job varchar(255) not null, path varchar(255), pipeline varchar(255) not null, project_id varchar(255), repository varchar(255), tag varchar(255), team varchar(255) not null, config clob, npm clob, primary key (build, job, pipeline, team));
create table firebase_test_report (successful boolean not null, timestamp timestamp(6), report_url varchar(65535), url varchar(65535), branch varchar(255), build varchar(255) not null, commit varchar(255), commit_author varchar(255), job varchar(255) not null, path varchar(255), pipeline varchar(255) not null, project_id varchar(255), repository varchar(255), tag varchar(255), team varchar(255) not null, test_branch varchar(255), test_commit varchar(255), test_commit_author varchar(255), test_path varchar(255), test_repository varchar(255), test_tag varchar(255), primary key (build, job, pipeline, team));
create table pull_request_analysis (number integer not null, timestamp timestamp(6), url varchar(65535), build varchar(255), commit varchar(255), commit_author varchar(255), job varchar(255), pipeline varchar(255), project_id varchar(255), repository varchar(255) not null, team varchar(255), manifest clob, primary key (number, repository));
create table terraform_analysis (pull_request_analysis_number integer not null, package_name varchar(255) not null, pull_request_analysis_repository varchar(255) not null, primary key (pull_request_analysis_number, package_name, pull_request_analysis_repository));
create table terraform_apply_execution (pull_request_analysis_number integer not null, create_timestamp timestamp(6), update_timestamp timestamp(6), package_name varchar(255) not null, pull_request_analysis_repository varchar(255) not null, state varchar(255) check (state in ('FAILED','PENDING','RUNNING','SUCCESS')), primary key (pull_request_analysis_number, package_name, pull_request_analysis_repository));
create table terraform_deployment (timestamp timestamp(6), url varchar(65535), branch varchar(255), build varchar(255) not null, commit varchar(255), commit_author varchar(255), job varchar(255) not null, package_name varchar(255), path varchar(255), pipeline varchar(255) not null, project_id varchar(255), repository varchar(255), tag varchar(255), team varchar(255) not null, params clob, primary key (build, job, pipeline, team));
create table terraform_plan_execution (pull_request_analysis_number integer not null, create_timestamp timestamp(6), update_timestamp timestamp(6), commit_id varchar(255), package_name varchar(255) not null, pull_request_analysis_repository varchar(255) not null, state varchar(255) check (state in ('FAILED','PENDING','RUNNING','SUCCESS')), primary key (pull_request_analysis_number, package_name, pull_request_analysis_repository));
create table terraform_plan_status (pull_request_analysis_number integer not null, commit_state varchar(255) check (commit_state in ('ERROR','FAILURE','PENDING','SUCCESS')), package_name varchar(255) not null, pull_request_analysis_repository varchar(255) not null, primary key (pull_request_analysis_number, package_name, pull_request_analysis_repository));
create index IDX53gv3w7abh93jc4c32mkxr1l0 on terraform_deployment (project_id, package_name);
alter table if exists cloud_run_analysis add constraint FKent61irs5eb8067raxy4kgy5s foreign key (pull_request_analysis_number, pull_request_analysis_repository) references pull_request_analysis;
alter table if exists firebase_app_analysis add constraint FK9vfpwwkoldq6n4k610bwe2hmi foreign key (pull_request_analysis_number, pull_request_analysis_repository) references pull_request_analysis;
alter table if exists terraform_analysis add constraint FK57uj5honb9qx8rddrk5r5etgu foreign key (pull_request_analysis_number, pull_request_analysis_repository) references pull_request_analysis;
alter table if exists terraform_apply_execution add constraint FK4g5eanhc5t7clq6tvtr39xg3y foreign key (pull_request_analysis_number, pull_request_analysis_repository) references pull_request_analysis;
alter table if exists terraform_plan_execution add constraint FKk6jpukcggh3htdg1wdbbf9hms foreign key (pull_request_analysis_number, pull_request_analysis_repository) references pull_request_analysis;
alter table if exists terraform_plan_status add constraint FKb25l65yk4dc57buagws6kqyoo foreign key (pull_request_analysis_number, pull_request_analysis_repository) references pull_request_analysis;
