CREATE EXTENSION IF NOT EXISTS vector;

create type article_status as enum ('draft', 'pending_review','off_shelf', 'no_kb','published', 'only_fans');
alter type article_status owner to postgres;
-- =============================================
-- 1. user
-- =============================================
create table "user"
(
    id             bigint                                              not null
        constraint users_pkey
            primary key,
    username       varchar(50),
    email          varchar(100),
    password_hash  varchar(255),
    nickname       varchar(30)   default '用户'::character varying     not null,
    bio            varchar(100)  default ''::character varying         not null,
    level          integer       default 0                             not null,
    experience     integer       default 0                             not null,
    roles          varchar(20)[] default '{user}'::character varying[] not null,
    permissions    varchar(50)[] default '{}'::character varying[]     not null,
    status         varchar(20)   default 'active'::character varying   not null,
    email_verified boolean       default false                         not null,
    created_at     timestamp     default now()                         not null,
    updated_at     timestamp     default now()                         not null,
    extra          jsonb         default '{}'::jsonb                   not null,
    phone          varchar(16),
    used_memory    integer       default 0                             not null,
    gender         varchar(2)    default ''::character varying,
    avatar_url     bigint        default 0
);

comment on table "user" is '用户表';

comment on column "user".id is '用户ID，主键';

comment on column "user".username is '用户名，唯一标识';

comment on column "user".email is '用户邮箱，唯一标识';

comment on column "user".password_hash is '密码哈希值';

comment on column "user".nickname is '用户昵称，显示名称';

comment on column "user".bio is '个人简介';

comment on column "user".level is '用户等级，从0开始';

comment on column "user".experience is '用户总经验值，只增不减';

comment on column "user".roles is '用户角色数组：user, admin';

comment on column "user".permissions is '用户权限数组';

comment on column "user".status is '用户状态：active, inactive, banned';

comment on column "user".email_verified is '邮箱验证状态';

comment on column "user".created_at is '创建时间';

comment on column "user".updated_at is '更新时间';

comment on column "user".extra is '扩充字段';

comment on column "user".phone is '电话号码';

comment on column "user".used_memory is '使用的存储';

COMMENT ON COLUMN "user".gender IS '性别：男, 女, 或空';
COMMENT ON COLUMN "user".avatar_url IS '用户头像的文件ID (关联file_url表)';


alter table "user"
    owner to postgres;

create unique index idx_users_email_unique
    on "user" (email)
    where (email IS NOT NULL);

create unique index idx_users_phone_unique
    on "user" (phone)
    where (phone IS NOT NULL);

create unique index idx_users_username_unique
    on "user" (username)
    where (username IS NOT NULL);

CREATE OR REPLACE FUNCTION update_user_level_on_exp()
    RETURNS TRIGGER AS $$
DECLARE
    new_level INTEGER;
    required_exp INTEGER;
BEGIN
    -- 如果经验值没有增加，直接返回
    IF NEW.experience <= OLD.experience THEN
        RETURN NEW;
    END IF;

    -- 初始化新等级为当前等级
    new_level := OLD.level;

    -- 循环计算应该达到的等级
    -- 从当前等级+1开始检查，直到找到经验值不足以升级的等级
    LOOP
        -- 计算下一级所需的累计经验值：2^(new_level + 1)
        required_exp := POWER(2, new_level + 1)::INTEGER;

        -- 如果当前经验值达到了下一级的要求，则升级
        IF NEW.experience >= required_exp THEN
            new_level := new_level + 1;
        ELSE
            -- 经验值不足以继续升级，退出循环
            EXIT;
        END IF;

        -- 防止无限循环（理论上不会发生，但作为安全措施）
        IF new_level > 100 THEN
            EXIT;
        END IF;
    END LOOP;

    -- 更新等级字段
    NEW.level := new_level;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 创建触发器
DROP TRIGGER IF EXISTS trigger_update_user_level ON "user";

CREATE TRIGGER trigger_update_user_level
    BEFORE UPDATE OF experience
    ON "user"
    FOR EACH ROW
EXECUTE FUNCTION update_user_level_on_exp();

-- 使用说明和测试示例
COMMENT ON FUNCTION update_user_level_on_exp() IS '
用户等级自动升级函数
升级规律：
- Level N: 2^N 经验
';





-- =============================================
-- 2. permission_group 表 (权限组)
-- =============================================
create table permission_group
(
    role            varchar,
    permission_list character varying[]
);
COMMENT ON COLUMN permission_group.role IS '角色名称 (如: admin, user)';
COMMENT ON COLUMN permission_group.permission_list IS '该角色拥有的权限代码列表';

comment on table permission_group is '角色和权限的对应';

alter table permission_group
    owner to postgres;
-- =============================================
-- 3. category 表 (分类)，暂时无作用
-- =============================================
create table category
(
    id         bigint                  not null,
    name       varchar(100)            not null,
    parent_id  bigint,
    created_at timestamp default now() not null,
    constraint categories_pkey primary key (id),
    constraint categories_name_key unique (name)
);
COMMENT ON COLUMN category.id IS '分类ID，主键';
COMMENT ON COLUMN category.name IS '分类名称';
COMMENT ON COLUMN category.parent_id IS '父分类ID (层级结构)';
COMMENT ON COLUMN category.created_at IS '创建时间';
alter table category
    owner to postgres;
-- =============================================
-- 4. post 表 (文章)
-- =============================================
create table post
(
    id              bigint                                         not null
        primary key,
    title           varchar(200)                                   not null,
    content         text           default ''::text                not null,
    summary         varchar(300),
    status          article_status default 'draft'::article_status not null,
    is_top          boolean        default false                   not null,
    is_recommend    boolean        default false                   not null,
    view_count      integer        default 1                       not null,
    like_count      integer        default 0                       not null,
    comment_count   integer        default 0                       not null,
    tags            varchar(12)[]  default ARRAY []::text[],
    user_id         bigint                                         not null,
    category_id     bigint,
    seo_keywords    varchar(500),
    seo_description text,
    created_at      timestamp      default now()                   not null,
    updated_at      timestamp      default now()                   not null,
    published_at    timestamp,
    extra           jsonb          default '{}'::jsonb,
    kb_id           bigint,
    vector          vector(1024),
    img             varchar,
    version         integer        default 1,
    like_ratio      numeric generated always as (
        CASE
            WHEN (view_count = 0) THEN (1)::numeric
            ELSE ((like_count)::numeric / (view_count)::numeric)
            END) stored,
    star_count      integer        default 0
);
COMMENT ON TABLE post IS '文章/帖子主表';
COMMENT ON COLUMN post.id IS '文章ID，主键';
COMMENT ON COLUMN post.title IS '文章标题';
COMMENT ON COLUMN post.content IS '文章原始内容 (Markdown/HTML)';
COMMENT ON COLUMN post.summary IS '文章摘要';
COMMENT ON COLUMN post.status IS '文章状态 (draft, published, etc.)';
COMMENT ON COLUMN post.is_top IS '是否置顶';
COMMENT ON COLUMN post.is_recommend IS '是否推荐';
COMMENT ON COLUMN post.view_count IS '阅读量';
COMMENT ON COLUMN post.like_count IS '点赞量';
COMMENT ON COLUMN post.comment_count IS '评论量';
COMMENT ON COLUMN post.tags IS '标签数组';
COMMENT ON COLUMN post.user_id IS '作者ID';
COMMENT ON COLUMN post.category_id IS '所属分类ID';
COMMENT ON COLUMN post.seo_keywords IS 'SEO关键词';
COMMENT ON COLUMN post.seo_description IS 'SEO描述';
COMMENT ON COLUMN post.created_at IS '创建时间';
COMMENT ON COLUMN post.updated_at IS '最后更新时间';
COMMENT ON COLUMN post.published_at IS '发布时间';
COMMENT ON COLUMN post.extra IS '扩展信息 (JSONB)';
COMMENT ON COLUMN post.kb_id IS '所属知识库ID (关联knowledge_base)';
COMMENT ON COLUMN post.vector IS '文章内容的向量数据 (用于语义搜索)';
COMMENT ON COLUMN post.img IS '文章封面图片路径/URL';
COMMENT ON COLUMN post.version IS '版本号 (用于乐观锁或版本控制)';
COMMENT ON COLUMN post.like_ratio IS '点赞率 (点赞数/阅读数，自动计算)';
COMMENT ON COLUMN post.star_count IS '收藏/标星数量';
alter table post
    owner to postgres;

create index idx_post_user_id
    on post (user_id);

create index idx_post_category_id
    on post (category_id);

create index idx_post_kb_id
    on post (kb_id);

create index idx_posts_cursor_status
    on post ((updated_at::date) desc, like_ratio desc, id desc)
    where (status >= 'published'::article_status);

create index idx_posts_vec_ivfflat
    on post using ivfflat (vector);
-- =============================================
-- 5. post_chunk_vector 表 (文章向量切片)
-- =============================================
create table post_chunk_vector
(
    id      bigint      not null
        constraint post_chunk_vectors_pkey
            primary key,
    post_id bigint      not null,
    hash    varchar(64) not null,
    chunk   text        not null,
    vector  vector(1024)
);
COMMENT ON TABLE post_chunk_vector IS '文章内容的向量切片 (用于RAG/AI搜索)';
COMMENT ON COLUMN post_chunk_vector.id IS '切片ID';
COMMENT ON COLUMN post_chunk_vector.post_id IS '所属文章ID';
COMMENT ON COLUMN post_chunk_vector.hash IS '切片内容的哈希值 (用于变更检测)';
COMMENT ON COLUMN post_chunk_vector.chunk IS '具体的文本切片内容';
COMMENT ON COLUMN post_chunk_vector.vector IS '该切片对应的向量数据';

alter table post_chunk_vector
    owner to postgres;

create index idx_post_chunk_vectors_post_id
    on post_chunk_vector (post_id);

create index idx_vec_ivfflat
    on post_chunk_vector using ivfflat (vector);
-- =============================================
-- 6. knowledge_base 表 (知识库)
-- =============================================
create table knowledge_base
(
    id         bigint                   not null
        constraint knowledge_bases_pkey
            primary key,
    user_id    bigint                   not null,
    name       varchar(100)             not null,
    created_at timestamp      default CURRENT_TIMESTAMP,
    status     article_status default 'draft'::article_status,
    index      text,
    like_count integer        default 0 not null,
    cover_url  varchar(100),
    post_count integer        default 0 not null
);
COMMENT ON TABLE knowledge_base IS '知识库/专栏';
COMMENT ON COLUMN knowledge_base.id IS '知识库ID';
COMMENT ON COLUMN knowledge_base.user_id IS '创建者/所有者ID';
COMMENT ON COLUMN knowledge_base.name IS '知识库名称';
COMMENT ON COLUMN knowledge_base.created_at IS '创建时间';
COMMENT ON COLUMN knowledge_base.status IS '知识库状态';
COMMENT ON COLUMN knowledge_base.index IS '知识库目录索引/结构数据';
COMMENT ON COLUMN knowledge_base.like_count IS '点赞/喜欢数量';
COMMENT ON COLUMN knowledge_base.cover_url IS '知识库封面图片URL';
COMMENT ON COLUMN knowledge_base.post_count IS '包含的文章数量';
alter table knowledge_base
    owner to postgres;

create index idx_kb_user_id
    on knowledge_base (user_id);

create index idx_knowledge_bases_cursor_status
    on knowledge_base ((created_at::date) desc, like_count desc, id desc)
    where (status >= 'published'::article_status);
-- =============================================
-- 7. directory 表 (知识库目录)
-- =============================================
create table directory
(
    id        bigint       not null,
    kb_id     bigint       not null,
    parent_id bigint,
    name      varchar(100) not null,
    post_id   bigint,
    sort_num  smallint default 0,
    user_id   bigint,
    -- 仅保留主键约束，确保 ID 唯一
    constraint directories_pkey primary key (id)
);
COMMENT ON TABLE directory IS '知识库的目录层级结构';
COMMENT ON COLUMN directory.id IS '目录节点ID';
COMMENT ON COLUMN directory.kb_id IS '所属知识库ID';
COMMENT ON COLUMN directory.parent_id IS '父节点ID (NULL表示根节点)';
COMMENT ON COLUMN directory.name IS '目录/章节名称';
COMMENT ON COLUMN directory.post_id IS '关联的文章ID (如果是叶子节点)';
COMMENT ON COLUMN directory.sort_num IS '排序序号 (越小越靠前)';
COMMENT ON COLUMN directory.user_id IS '创建者ID';
alter table directory
    owner to postgres;

create index idx_dir_kb_id
    on directory (kb_id);

create index idx_dir_parent_id
    on directory (parent_id);

create index idx_dir_post_id
    on directory (post_id);
-- =============================================
-- 8. user_log 表 (用户日志)
-- =============================================
create table user_log
(
    id         bigint                  not null
        constraint user_logs_pkey
            primary key,
    user_id    bigint                  not null,
    action     varchar(50)             not null,
    device     varchar(10)             not null,
    info       jsonb,
    created_at timestamp default now() not null,
    processed  boolean   default false not null
);
COMMENT ON TABLE user_log IS '用户操作/审计日志';
COMMENT ON COLUMN user_log.user_id IS '用户ID';
COMMENT ON COLUMN user_log.action IS '行为动作 (如: login, update_profile)';
COMMENT ON COLUMN user_log.device IS '操作设备 (如: ios, android, web)';
COMMENT ON COLUMN user_log.info IS '日志详情 (JSONB格式)';
COMMENT ON COLUMN user_log.processed IS '是否已被数据分析任务处理';
alter table user_log
    owner to postgres;

create index user_logs_processed_false_idx
    on user_log (processed)
    where (processed = false);
-- =============================================
-- 9. user_extra_info 热信息
-- =============================================
create table user_extra_info
(
    user_id            bigint            not null
        constraint user_extra_infos_pkey
            primary key,
    follower_count     integer default 0 not null,
    following_count    integer default 0 not null,
    like_count         integer default 0 not null,
    comment_count      integer default 0 not null,
    post_count         integer default 0 not null,
    interest_vector    vector(1024),
    article_view_count integer default 0 not null,
    msg_count          integer default 0,
    msg_reply_count    integer default 0,
    msg_like_count     integer default 0,
    msg_report_count   integer default 0,
    msg_message_count  integer default 0
);

comment on table user_extra_info is '用户热信息，这些是系统自动维护的';
comment on column user_extra_info.user_id is '用户ID，关联users表主键，唯一标识用户';
comment on column user_extra_info.follower_count is '粉丝数：当前用户的粉丝总数量，默认值0';
comment on column user_extra_info.following_count is '关注数：当前用户关注的其他用户数量，默认值0';
comment on column user_extra_info.like_count is '点赞数：用户累计点赞的内容（帖子/评论/文章）总数，默认值0';
comment on column user_extra_info.comment_count is '评论数：用户累计发布的评论总数，默认值0';
comment on column user_extra_info.post_count is '发帖数：用户累计发布的帖子总数，默认值0';
comment on column user_extra_info.interest_vector is '用户兴趣向量：1024维浮点型向量，用于个性化推荐、兴趣画像分析';
comment on column user_extra_info.article_view_count is '文章浏览量：用户累计浏览的文章数量，默认值0';
COMMENT ON COLUMN user_extra_info.msg_count IS '消息总数';
COMMENT ON COLUMN user_extra_info.msg_reply_count IS '收到的回复消息数量';
COMMENT ON COLUMN user_extra_info.msg_like_count IS '收到的点赞消息数量';
COMMENT ON COLUMN user_extra_info.msg_report_count IS '收到的举报/系统通知数量';
COMMENT ON COLUMN user_extra_info.msg_message_count IS '收到的私信/聊天数量';

alter table user_extra_info
    owner to postgres;
-- =============================================
-- 10. file_url 表 (文件映射到oos)
-- =============================================
create table file_url
(
    short_url  bigint not null
        constraint file_url_pk
            primary key,
    user_id    bigint not null,
    url        bytea,
    name       varchar(255) default '未命名文件'::character varying,
    created_at date         default now(),
    file_size  integer
);

alter table file_url
    owner to postgres;

create index idx_file_urls_url_hash
    on file_url using hash (url);

create index file_url_user_index
    on file_url (user_id, short_url);

COMMENT ON COLUMN file_url.short_url IS '唯一id，前端通过他访问';
COMMENT ON COLUMN file_url.user_id IS '文件拥有者';
COMMENT ON COLUMN file_url.url IS '对象存储的实际路径';
COMMENT ON COLUMN file_url.name IS '文件名';
COMMENT ON COLUMN file_url.file_size IS '文件大小，用于控制存储空间';

-- =============================================
--  post__history 表 (文章历史版本)
-- =============================================
create table post_history
(
    id         bigint            not null
        constraint posts_history_pk
            primary key,
    post_id    bigint,
    content    text,
    created_at timestamp,
    user_id    bigint            not null,
    version    integer default 0 not null
);

comment on column post_history.post_id is '文章id';

comment on column post_history.content is '内容';

comment on column post_history.user_id is '文章的拥有者id';

comment on column post_history.version is '版本';

alter table post_history
    owner to postgres;

create index posts_history_post_id_index
    on post_history (post_id);
-- =============================================
--  post_operation 表 (文章操作)
-- =============================================
create table post_operation
(
    id           bigint not null
        constraint post_operations_pk
            primary key,
    user_id      bigint not null,
    target_id    bigint not null,
    operate_type smallint
);
COMMENT ON TABLE post_operation IS '用户对文章的操作记录 (如点赞、收藏等)';
COMMENT ON COLUMN post_operation.user_id IS '操作用户ID';
COMMENT ON COLUMN post_operation.target_id IS '目标对象ID (通常是post_id)';
COMMENT ON COLUMN post_operation.operate_type IS '操作类型代码 (需参考枚举定义)';
alter table post_operation
    owner to postgres;

create unique index post_type_index
    on post_operation (user_id, target_id, operate_type);
-- =============================================
--  user_follow 表 (关注关系)
-- =============================================
create table user_follow
(
    follower_id bigint not null,
    followee_id bigint not null,
    id          bigint not null
        constraint users_follow_pk
            primary key
);
COMMENT ON TABLE user_follow IS '用户关注关系表';
COMMENT ON COLUMN user_follow.follower_id IS '粉丝ID (谁点的关注)';
COMMENT ON COLUMN user_follow.followee_id IS '被关注者ID';
alter table user_follow
    owner to postgres;

create index users_follow_followee_id_index
    on user_follow (followee_id);

create index users_follow_follower_id_index_2
    on user_follow (follower_id);
-- =============================================
--  carousel 表 (轮播图)
-- =============================================
create table carousel
(
    date        date default now() not null,
    id          bigint             not null
        constraint carousels_pk
            primary key,
    title       varchar,
    description varchar,
    url         varchar,
    cover_url   varchar,
    color       varchar
);
COMMENT ON TABLE carousel IS '首页/页面轮播图配置';
COMMENT ON COLUMN carousel.date IS '生效日期/创建日期';
COMMENT ON COLUMN carousel.title IS '轮播图标题';
COMMENT ON COLUMN carousel.description IS '轮播图描述';
COMMENT ON COLUMN carousel.url IS '点击跳转的目标URL';
COMMENT ON COLUMN carousel.cover_url IS '展示图片的URL';
COMMENT ON COLUMN carousel.color IS '背景主色调 (用于UI渐变)';
alter table carousel
    owner to postgres;

create index carousels_date_index
    on carousel (date desc);
-- =============================================
-- paragraph_comment 表 (段评)
-- =============================================
create table paragraph_comment
(
    id          bigint                  not null
        constraint paragraph_comments_pkey
            primary key,
    user_id     bigint                  not null,
    data_id     integer                 not null,
    post_id     bigint                  not null,
    content     varchar(255)            not null,
    version     integer,
    like_count  integer   default 0     not null,
    created_at  timestamp default now() not null,
    reply_count integer   default 0
);
COMMENT ON TABLE paragraph_comment IS '文章段落评论';
COMMENT ON COLUMN paragraph_comment.data_id IS '段落标识ID (由前端计算或分配)';
COMMENT ON COLUMN paragraph_comment.post_id IS '所属文章ID';
COMMENT ON COLUMN paragraph_comment.content IS '评论内容';
COMMENT ON COLUMN paragraph_comment.version IS '对应的文章版本';
COMMENT ON COLUMN paragraph_comment.reply_count IS '回复数量';
comment on column paragraph_comment.data_id is '该字段由前端填充';

alter table paragraph_comment
    owner to postgres;

create index idx_post_paragraph
    on paragraph_comment (post_id asc, data_id asc, version asc, like_count desc, created_at desc, id desc);
-- =============================================
--  paragraph 表 (段落映射)
-- =============================================
create table paragraph
(
    post_id bigint            not null,
    count   integer default 1 not null,
    data_id integer           not null,
    version integer           not null,
    id      bigint            not null
        constraint paragraphs_pk
            primary key
);
COMMENT ON TABLE paragraph IS '文章段落结构映射表';
COMMENT ON COLUMN paragraph.post_id IS '文章ID';
COMMENT ON COLUMN paragraph.count IS '该段落下的评论总数';
COMMENT ON COLUMN paragraph.data_id IS '段落标识ID';
COMMENT ON COLUMN paragraph.version IS '文章版本';
alter table paragraph
    owner to postgres;

create unique index idx_paragraphs_post_id_data_id_version
    on paragraph (post_id, data_id, version);
-- =============================================
--  post_comment 表 (整文评论)
-- =============================================
create table post_comment
(
    id          bigint              not null
        constraint posts_comment_pk
            primary key,
    content     varchar(255)        not null,
    post_id     bigint              not null,
    version     integer             not null,
    user_id     bigint              not null,
    created_at  timestamp default now(),
    like_count  integer   default 0,
    reply_count integer   default 0 not null
);
COMMENT ON TABLE post_comment IS '文章底部评论 (非段评)';
COMMENT ON COLUMN post_comment.content IS '评论内容';
COMMENT ON COLUMN post_comment.post_id IS '文章ID';
COMMENT ON COLUMN post_comment.user_id IS '评论者ID';
COMMENT ON COLUMN post_comment.version IS '文章版本';
COMMENT ON COLUMN post_comment.reply_count IS '子回复数量';
alter table post_comment
    owner to postgres;

create index idx_posts_comment_post_id_like_create
    on post_comment (post_id asc, like_count desc, created_at desc, id desc);

-- =============================================
--  reply 表
-- =============================================
create table reply
(
    id               bigint                       not null,
    par_comment_id   bigint,
    user_id          bigint,
    content          varchar(255)                 not null,
    like_count       integer default 0            not null,
    created_at       timestamp    default now() not null,
    post_comment_id  bigint,
    reply_id         bigint,
    reply_comment_id bigint
);

comment on column reply.par_comment_id is '段评的id';

comment on column reply.user_id is '创建评论的用户id';

comment on column reply.content is '内容';

comment on column reply.like_count is '点赞数';

comment on column reply.created_at is '创建时间';

comment on column reply.post_comment_id is '文章评论id和段落评论id二选一';

comment on column reply.reply_id is '回复的用户id';

COMMENT ON TABLE reply IS '评论的子回复 (楼中楼)';
COMMENT ON COLUMN reply.reply_comment_id IS '被回复的各种类型评论的原始ID (父级ID)';
alter table reply
    owner to postgres;

create index replys_par_index
    on reply (par_comment_id asc, like_count desc, like_count desc, id desc);

create index replys_post_comment_index
    on reply (post_comment_id asc, like_count desc, created_at desc, id desc);
-- =============================================
--  user_collect 表 (收藏)
-- =============================================
create table user_collect
(
    user_id    bigint                  not null,
    target_id  bigint,
    group_id   bigint,
    id         bigint                  not null
        constraint users_collect_pk
            primary key,
    created_at timestamp default now() not null
);

comment on column user_collect.user_id is '创建的用户';

comment on column user_collect.target_id is '收藏的具体物品的id';

comment on column user_collect.group_id is '收藏组的id';

alter table user_collect
    owner to postgres;

create index users_collect_user_id_index
    on user_collect (user_id);
-- =============================================
--  user_collect_group表 (收藏组)
-- =============================================
create table user_collect_group
(
    id          bigint      not null
        constraint users_collect_group_pk
            primary key,
    name        varchar(50) not null,
    user_id     bigint      not null,
    type        smallint    not null,
    publish     boolean      default false,
    description varchar(100) default ''::character varying,
    count       integer      default 0
);

comment on column user_collect_group.name is '名称';

comment on column user_collect_group.type is '收藏组的类型';

comment on column user_collect_group.publish is '是否公开';

comment on column user_collect_group.count is '收藏的数量';

alter table user_collect_group
    owner to postgres;






-- =============================================
-- 17. post_view_history 表 (浏览历史)
-- =============================================
create table post_view_history
(
    user_id     bigint                  not null,
    post_id     bigint                  not null,
    browse_time timestamp default now() not null,
    id          bigint                  not null
        constraint post_view_history_pk
            primary key,
    count       integer   default 0     not null
);

COMMENT ON TABLE post_view_history IS '用户文章浏览历史';
COMMENT ON COLUMN post_view_history.user_id IS '用户ID';
COMMENT ON COLUMN post_view_history.post_id IS '文章ID';
COMMENT ON COLUMN post_view_history.browse_time IS '最近浏览时间';
COMMENT ON COLUMN post_view_history.count IS '浏览次数';
alter table post_view_history
    owner to postgres;

create unique index post_view_history_post_id_index
    on post_view_history (user_id, post_id);


-- =============================================
--  user_notice 表 (用户通知)
-- =============================================
create table user_notice
(
    id          bigint   not null
        constraint user_notice_pk
            primary key,
    type        smallint not null,
    target_id   bigint   not null,
    user_id     bigint,
    target_user bigint,
    checked     boolean   default false,
    created_at  timestamp default now()
);

COMMENT ON COLUMN user_notice.checked IS '是否已读 (false=未读, true=已读)';

comment on column user_notice.type is '类型';

comment on column user_notice.target_id is '通知的具体物品id';

comment on column user_notice.user_id is '被通知的用户';

comment on column user_notice.target_user is '创建通知的用户';

alter table user_notice
    owner to postgres;

create index user_notice_user_id_index
    on user_notice (user_id asc, type asc, id desc);

create table user_oauth
(
    id               bigint not null
        constraint user_oauth_pk
            primary key,
    provider         smallint,
    provider_user_id varchar,
    access_token     varchar,
    refresh_token    varchar,
    expires_at       varchar,
    created_at       date default CURRENT_DATE,
    user_id          bigint
);




-- =============================================
-- 19. user_oauth 表 (第三方登录)
-- =============================================
COMMENT ON TABLE user_oauth IS '第三方OAuth登录信息';
COMMENT ON COLUMN user_oauth.provider IS '提供商代码 (如: 1=Google, 2=Github)';
COMMENT ON COLUMN user_oauth.provider_user_id IS '第三方平台的用户唯一标识 (OpenID)';
COMMENT ON COLUMN user_oauth.access_token IS '访问令牌';
COMMENT ON COLUMN user_oauth.refresh_token IS '刷新令牌';
COMMENT ON COLUMN user_oauth.expires_at IS '令牌过期时间';
COMMENT ON COLUMN user_oauth.user_id IS '关联的内部用户ID';
alter table user_oauth
    owner to postgres;

create unique index user_oauth_id_provider_user_id_index
    on user_oauth (provider_user_id, provider);


