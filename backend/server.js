const express = require('express');
const cors = require('cors');
const multer = require('multer');
const path = require('path');
const db = require('./db');
const morgan = require('morgan');

const app = express();
app.use(cors());
app.use(express.json());
app.use(morgan('dev'));

// 1. 设置文件存储目录 (满足“数据库存储文件”要求)
const storage = multer.diskStorage({
  destination: (req, file, cb) => cb(null, 'uploads/'),
  filename: (req, file, cb) => cb(null, Date.now() + path.extname(file.originalname))
});
const upload = multer({ storage });

// --- 数据库交互接口 ---

// 2. 统计查询 (调用视图: user_weekly_report)
app.get('/api/report/weekly/:userId', async (req, res) => {
  try {
    const [rows] = await db.execute('SELECT * FROM user_weekly_report WHERE user_id = ?', [req.params.userId]);
    res.json(rows);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 3. 健康趋势 (调用视图: user_health_trend)
app.get('/api/report/health/:userId', async (req, res) => {
  try {
    const [rows] = await db.execute('SELECT * FROM user_health_trend WHERE user_id = ?', [req.params.userId]);
    res.json(rows);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 4. 打卡记录 (插入 record 表，这将自动触发 tr_record_insert 触发器维护统计表)
app.post('/api/record', async (req, res) => {
  const { user_habit_id, record_date, fact_value, record_status, note } = req.body;
  try {
    const [result] = await db.execute(
      'INSERT INTO record (user_habit_id, record_date, fact_value, record_status, note) VALUES (?, ?, ?, ?, ?)',
      [user_habit_id, record_date, fact_value, record_status, note]
    );
    res.json({ success: true, id: result.insertId, message: "记录已保存，统计表自动同步中" });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 5. 生成月报 (调用存储过程: monthly_report)
app.get('/api/report/monthly/:userId/:yearMonth', async (req, res) => {
  try {
    const [results] = await db.query('CALL monthly_report(?, ?)', [req.params.userId, req.params.userId]);
    res.json(results[0]);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 6. 文件上传并关联到附件表
app.post('/api/upload', upload.single('file'), async (req, res) => {
  const { user_id, relate_type, relate_id } = req.body;
  const file = req.file;
  if (!file) return res.status(400).send('No file uploaded.');

  try {
    await db.execute(
      'INSERT INTO attachment (user_id, file_name, file_path, file_type, file_size, relate_type, relate_id) VALUES (?, ?, ?, ?, ?, ?, ?)',
      [user_id, file.originalname, file.path, file.mimetype, file.size, relate_type, relate_id]
    );
    res.json({ success: true, path: file.path });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

const PORT = 3001;
app.listen(PORT, () => console.log(`Backend running on port ${PORT}`));
