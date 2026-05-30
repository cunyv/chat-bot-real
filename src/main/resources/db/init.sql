-- 创建数据库
CREATE DATABASE IF NOT EXISTS customer_service DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE customer_service;

-- 知识库表
CREATE TABLE IF NOT EXISTS knowledge_base (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category VARCHAR(50) NOT NULL COMMENT '分类: COACH-教练, VENUE-场地',
    title VARCHAR(255) NOT NULL COMMENT '标题',
    content TEXT NOT NULL COMMENT '内容',
    metadata JSON COMMENT '元数据(专长、设施、价格等)',
    embedding LONGBLOB COMMENT '向量数据',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE, INACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_category (category),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库表';

-- 会话表
CREATE TABLE IF NOT EXISTS chat_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL UNIQUE COMMENT '会话ID',
    user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
    title VARCHAR(255) COMMENT '会话标题',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE, CLOSED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话表';

-- 对话历史表
CREATE TABLE IF NOT EXISTS conversation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL COMMENT '会话ID',
    user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
    role VARCHAR(20) NOT NULL COMMENT '角色: USER, ASSISTANT, SYSTEM',
    content TEXT NOT NULL COMMENT '消息内容',
    retrieved_docs JSON COMMENT '检索到的文档ID列表',
    token_count INT COMMENT 'Token数量',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_session_id (session_id),
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话历史表';

-- 插入教练知识示例数据
INSERT INTO knowledge_base (category, title, content, metadata) VALUES
('COACH', '张教练 - 篮球专项', '张教练，国家一级篮球运动员，10年教学经验。擅长青少年篮球基础训练、投篮技术纠正、比赛战术指导。曾在省队担任助理教练，培养过多位优秀球员。授课风格耐心细致，善于因材施教。', '{"specialty":"篮球","experience":"10年","level":"国家一级","price":300,"location":"北京","rating":4.9}'),
('COACH', '李教练 - 网球专项', '李教练，ITF认证网球教练，8年专业教学经验。专注于网球基本功训练、发球技术提升、比赛心理辅导。曾带队参加多项业余赛事并获得优异成绩。', '{"specialty":"网球","experience":"8年","level":"ITF认证","price":350,"location":"上海","rating":4.8}'),
('COACH', '王教练 - 游泳专项', '王教练，前省队游泳运动员，12年游泳教学经验。擅长各泳姿教学、水上安全培训、体能康复训练。教学方法科学系统，适合各年龄段学员。', '{"specialty":"游泳","experience":"12年","level":"前省队","price":280,"location":"广州","rating":4.9}'),
('COACH', '赵教练 - 瑜伽专项', '赵教练，RYT500认证瑜伽导师，6年教学经验。精通哈他瑜伽、流瑜伽、阴瑜伽等多种流派。擅长体态矫正、减压放松、孕期瑜伽。', '{"specialty":"瑜伽","experience":"6年","level":"RYT500","price":250,"location":"深圳","rating":4.7}'),
('COACH', '刘教练 - 足球专项', '刘教练，亚足联B级教练员，15年足球执教经验。曾任职业俱乐部梯队教练，擅长青训体系搭建、战术分析、球员技术提升。', '{"specialty":"足球","experience":"15年","level":"亚足联B级","price":400,"location":"北京","rating":4.9}');

-- 插入场地知识示例数据
INSERT INTO knowledge_base (category, title, content, metadata) VALUES
('VENUE', '星耀篮球馆', '星耀篮球馆位于北京市朝阳区，拥有4块标准篮球场，采用专业枫木地板，配备LED照明系统和空调。馆内设有休息区、更衣室、淋浴间。营业时间：8:00-22:00。支持包场、散客、培训课程。', '{"type":"篮球","location":"北京朝阳","courts":4,"price":200,"facilities":["空调","更衣室","淋浴","停车场"],"rating":4.8}'),
('VENUE', '银河网球中心', '银河网球中心位于上海市浦东新区，设有6片室内网球场和4片室外球场。室内场采用硬地丙烯酸面层，配备专业灯光。提供球拍租赁、教练预约、赛事举办等服务。', '{"type":"网球","location":"上海浦东","courts":10,"price":150,"facilities":["室内场","灯光","租赁","停车"],"rating":4.7}'),
('VENUE', '碧波游泳馆', '碧波游泳馆位于广州市天河区，拥有50米标准泳池和25米训练泳池各一个。水质达到国际标准，配备专业救生团队。设有儿童戏水区、桑拿房、健身区。', '{"type":"游泳","location":"广州天河","pools":2,"price":80,"facilities":["标准池","救生","桑拿","健身"],"rating":4.9}'),
('VENUE', '静心瑜伽会所', '静心瑜伽会所位于深圳市南山区，环境优雅安静。设有3间专业瑜伽教室，提供高温瑜伽、空中瑜伽等多种课程。配有冥想室、茶歇区、有机素食餐厅。', '{"type":"瑜伽","location":"深圳南山","rooms":3,"price":120,"facilities":["高温房","冥想室","茶歇","餐厅"],"rating":4.8}'),
('VENUE', '绿茵足球公园', '绿茵足球公园位于北京市通州区，拥有2块11人制标准足球场和3块5人制小场。采用进口人造草皮，配备夜间照明。提供赛事组织、团建活动、青少年培训等服务。', '{"type":"足球","location":"北京通州","courts":5,"price":500,"facilities":["标准场","灯光","停车","淋浴"],"rating":4.6}');
