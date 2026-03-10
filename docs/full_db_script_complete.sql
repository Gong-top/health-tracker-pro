-- ==========================================================
-- 个人健康管理与习惯追踪系统 - 完整数据库脚本 (严格保留原代码版)
-- ==========================================================

-- 1. 数据库初始化 (Railway 环境通常使用名为 railway 的数据库)
create database if not exists railway default character set utf8mb4;
use railway;

-- 2. 清理旧表 (如果存在)
set foreign_key_checks = 0;
drop table if exists attachment;
drop table if exists statistic;
drop table if exists exercise_record;
drop table if exists sports;
drop table if exists health_data;
drop table if exists record;
drop table if exists user_habit;
drop table if exists habit;
drop table if exists users;
set foreign_key_checks = 1;

-- ---------------------------------------------------------
-- 3. 原始建表语句 (严格保留你的字段名和逻辑)
-- ---------------------------------------------------------

-- 1. 用户表
create table users(
    id bigint primary key auto_increment,
    username varchar(50) unique not null comment '用户名',
    password varchar(255) not null comment '密码',
    nickname varchar(50) comment '昵称',
    email varchar(100) unique comment '电子邮箱',
    photo varchar(500) comment '头像',
    birthday date comment '出生日期',
    gender enum('male','female') default 'female' comment '性别',
    create_time timestamp default current_timestamp comment '创建时间',
    index idx_username(username)
)comment='用户表';

-- 2. 习惯定义表
create table habit(
    habit_id bigint primary key auto_increment,
    habit_name varchar(100) not null comment '习惯名',
    description text comment '习惯描述',
    habit_sign varchar(50) comment '图标',
    category varchar(50) comment '喜欢类别',
    habit_status boolean default true comment '是否启用',
    create_time timestamp default current_timestamp comment '创建时间',
    index idx_category(category)
)comment='习惯定义表';

-- 3. 用户习惯表
create table user_habit(
    user_habit_id bigint primary key auto_increment,
    user_id bigint not null,
    habit_id bigint not null,
    custom_name varchar(100) comment '用户自定义习惯名',
    target_value decimal(10,2) not null default 1 comment '每日/每次目标值',
    target_unit varchar(20) not null default '次' comment '目标单位',
    frequency_interval int not null default 1 comment '频率间隔',
    frequency_unit enum('day','week','month') not null default 'day' comment '频率间隔单位',
    reminder_time time comment '提醒时间',
    user_habit_status boolean default true comment '该习惯是否对用户有效',
    start_date date comment '习惯开始时间',
    end_date date comment '习惯结束时间',
    create_time timestamp default current_timestamp,
    update_time timestamp default current_timestamp on update current_timestamp,
    foreign key(user_id) references users(id) on delete cascade,
    unique key userhabit(user_id,habit_id),
    index idx_idstatus(user_id,user_habit_status)
)comment='用户个人习惯表';

-- 4. 习惯记录表
create table record(
    record_id bigint primary key auto_increment,
    user_habit_id bigint not null comment '用户习惯id',
    record_date date not null comment '记录日期',
    fact_value decimal(10,2) not null default 0 comment '实际完成值',
    record_status tinyint not null default 0 comment '状态（0为未完成，1为已完成）',
    finish_time timestamp null default null,
    create_time timestamp default current_timestamp,
    update_time timestamp default current_timestamp on update current_timestamp,
    note text comment '备注',
    foreign key(user_habit_id) references user_habit(user_habit_id) on delete cascade,
    unique key iddate(user_habit_id,record_date),
    index idx_userdate(user_habit_id,record_date)
)comment='习惯打卡表';

-- 5. 健康数据表
create table health_data(
    data_id bigint primary key auto_increment,
    user_id bigint not null,
    data_type enum('weight','heart_rate','blood_pressure','systolic','diastolic','body_temperature',
    'respiratory_rate','steps','calories','sleep_duration','deep_sleep_duration','light_sleep_duration'
    )not null comment '增加 systolic 和 diastolic 支持血压分开存',
    data_value decimal(10,2) not null comment '数据值',
    data_unit varchar(20) default '' comment '数值单位',
    record_date date not null comment '记录日期',
    record_time time comment '记录时间',
    source varchar(50) default 'manual' comment '数据来源',
    remark varchar(200) comment '备注',
    create_time timestamp default current_timestamp comment '创建时间',
    foreign key(user_id) references users(id) on delete cascade,
    index idx_iddate(user_id,record_date)
)comment='健康数据表';

-- 6. 运动项目定义表
create table sports(
    sports_id bigint primary key auto_increment,
    sports_name varchar(100) not null unique,
    description text comment '运动描述',
    category varchar(50) comment '运动类别',
    sports_status boolean default true,
    create_time timestamp default current_timestamp
)comment='运动项目集合';

-- 7. 运动记录表
create table exercise_record(
    exercise_record_id bigint primary key auto_increment,
    user_id bigint not null,
    sports_id bigint not null,
    sports_type varchar(50) default '其他' comment '冗余存储运动名称，如：跑步',
    duration int not null comment '运动时长（分钟）',
    calories decimal(8,2) comment '卡路里消耗（千卡）',
    feeling enum('very_good','good','normal','tired','very_tired') comment '运动感受',
    exercise_record_date date not null,
    note text comment '备注',
    create_time timestamp default current_timestamp,
    foreign key(user_id) references users(id) on delete cascade,
    foreign key(sports_id) references sports(sports_id),
    index idx_userdate(user_id,exercise_record_date)
)comment='运动记录表';

-- 8. 统计表
create table statistic(
    id bigint primary key auto_increment,
    user_id bigint not null,
    sta_date date not null comment '统计日期',
    total_habits int default 0 comment '当日应完成习惯总数',
    finished_habits int default 0 comment '当日已完成完成习惯数',
    total_exercise_duration int default 0 comment '当日运动总时长(分钟)',
    total_calories decimal(10,2) default 0.0 comment '当日消耗总卡路里（大卡）',
    mood_score int default null comment '心情指数(1-10)',
    weight decimal(6,2) default null comment '当日体重(公斤)',
    create_time timestamp default current_timestamp,
    update_time timestamp default current_timestamp on update current_timestamp,
    foreign key(user_id) references users(id) on delete cascade,
    unique key iddate(user_id,sta_date),
    index idx_userdate(user_id,sta_date),
    constraint c1 check(mood_score is null or(mood_score between 1 and 10))
)comment='统计表';

-- 9. 附件表
create table attachment(
    attachment_id bigint primary key auto_increment,
    user_id bigint not null,
    file_name varchar(255) not null,
    file_path varchar(500) not null,
    file_type varchar(100) not null,
    file_size bigint not null,
    relate_type enum('health_data','exercise_record','record','general')default 'general',
    relate_id bigint,
    upload_time timestamp default current_timestamp,
    remark varchar(500),
    create_time timestamp default current_timestamp,
    foreign key(user_id) references users(id) on delete cascade
)comment='用户附件表';

-- ---------------------------------------------------------
-- 4. 视图 (保留你的逻辑)
-- ---------------------------------------------------------
create or replace view user_weekly_report as
select
    uh.user_id,
    uh.user_habit_id,
    coalesce(uh.custom_name,h.habit_name)as fact_name,
    uh.target_value as daily_target,
    uh.target_unit,
    case
        when uh.frequency_unit='day'then round(uh.target_value*7,1)
        when uh.frequency_unit='week'then round(uh.target_value,1)
        else round(uh.target_value,1)
    end as weekly_target,
    coalesce(sum(r.fact_value),0)as total_finished,
    round(
        coalesce(
            (sum(r.fact_value)/
            case
                when uh.frequency_unit='day'then(uh.target_value*7)
                when uh.frequency_unit='week'then uh.target_value
                else uh.target_value
            end
            )*100,
            0),
        1)as completion_rate_num
from user_habit uh
join habit h on uh.habit_id=h.habit_id
left join record r on uh.user_habit_id=r.user_habit_id
    and r.record_date between date_sub(curdate(),interval 6 day)and curdate()
where uh.user_habit_status=true
group BY uh.user_habit_id;

-- ---------------------------------------------------------
-- 5. 触发器 (保留你的逻辑)
-- ---------------------------------------------------------
delimiter $$

-- 1. 插入触发器
drop trigger if exists tr_record_insert $$
create trigger tr_record_insert
after insert on record
for each row
begin
    declare v_user_id bigint;
    declare v_total_habits int default 0;
    select user_id into v_user_id from user_habit where user_habit_id = new.user_habit_id;
    select count(*) into v_total_habits from user_habit 
    where user_id = v_user_id and user_habit_status = true;
    
    insert into statistic (user_id, sta_date, total_habits, finished_habits)
    values (v_user_id, new.record_date, v_total_habits, case when new.record_status = 1 then 1 else 0 end)
    on duplicate key update
        finished_habits = finished_habits + (case when new.record_status = 1 then 1 else 0 end),
        total_habits = v_total_habits;
end $$

-- 2. 更新触发器
drop trigger if exists tr_record_update $$
create trigger tr_record_update
after update on record
for each row
begin
    declare v_user_id bigint;
    select user_id into v_user_id from user_habit where user_habit_id = new.user_habit_id;
    if old.record_status = 0 and new.record_status = 1 then
        update statistic set finished_habits = finished_habits + 1 
        where user_id = v_user_id and sta_date = new.record_date;
    elseif old.record_status = 1 and new.record_status = 0 then
        update statistic set finished_habits = greatest(finished_habits - 1, 0) 
        where user_id = v_user_id and sta_date = new.record_date;
    end if;
end $$

-- 3. 删除触发器
drop trigger if exists tr_record_delete $$
create trigger tr_record_delete
after delete on record
for each row
begin
    update statistic 
    set finished_habits = greatest(finished_habits - (case when old.record_status = 1 then 1 else 0 end), 0)
    where sta_date = old.record_date;
end $$

delimiter ;

-- ---------------------------------------------------------
-- 6. 存储过程 (保留你的逻辑)
-- ---------------------------------------------------------
delimiter $$

drop procedure if exists monthly_report $$
create procedure monthly_report(
    in p_user_id bigint,
    in p_year_month char(7)
)
begin
    select
        '习惯打卡' as 类型,
        fact_name as 项目,
        sum(total_finished) as 月度累计完成,
        concat(round(avg(completion_rate_num), 1), '%') as 平均达成率
    from user_weekly_report
    where user_id = p_user_id
    group by fact_name
    
    union all
    
    select 
        '健身运动' as 类型,
        sports_type as 项目,
        sum(duration) as 总时长_分钟,
        sum(calories) as 总消耗_kcal
    from exercise_record
    where user_id = p_user_id 
      and date_format(exercise_record_date, '%y-%m') = p_year_month
    group by sports_type;
end $$

delimiter ;

-- ---------------------------------------------------------
-- 7. 初始数据
-- ---------------------------------------------------------
insert ignore into sports(sports_id,sports_name)values(1,'常规锻炼');
insert ignore into users(id,username,password,nickname,email)values(1,'admin','123456','健康达人','admin@example.com');
insert ignore into habit(habit_name,category)values('早起','生活'),('阅读','学习'),('多喝水','健康');
