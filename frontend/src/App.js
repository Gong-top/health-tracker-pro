import React, { useState, useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Link, Navigate } from 'react-router-dom';
import Dashboard from './Dashboard';
import { LayoutDashboard, CheckSquare, Settings, Activity, LogOut, Heart, Plus, X, Trash2, Camera, Paperclip, Clock, Calendar, Database, UserCircle, Save, Edit3, Mail, User, Info, Flame, Thermometer, History, TrendingUp, CheckCircle, ExternalLink, Lock, UserPlus } from 'lucide-react';

const API_BASE = "https://health-tracker-pro-production.up.railway.app/api";

// --- 用户认证组件 ---
const Auth = ({ setAuth, setUserId, setNickname }) => {
  const [isLogin, setIsLogin] = useState(true);
  const [formData, setFormData] = useState({ username: '', password: '', nickname: '', email: '' });
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    const endpoint = isLogin ? '/login' : '/register';
    try {
      const res = await fetch(`${API_BASE}${endpoint}`, {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(formData)
      });
      const data = await res.json();
      if (res.ok) {
        if (isLogin) {
          setAuth(true);
          setUserId(data.id);
          setNickname(data.nickname);
          localStorage.setItem('health_user', JSON.stringify(data));
        } else {
          alert("注册成功，请登录！");
          setIsLogin(true);
        }
      } else {
        setError(data.error || '操作失败');
      }
    } catch (err) {
      setError('连接后端失败，请检查服务是否启动');
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 p-6 relative overflow-hidden">
      <div className="absolute top-[-10%] right-[-10%] w-[40%] aspect-square bg-indigo-100 rounded-full blur-3xl opacity-50"></div>
      <div className="max-w-md w-full bg-white p-12 rounded-[4rem] shadow-2xl space-y-8 relative z-10 border border-gray-100">
        <div className="text-center space-y-4">
          <div className="w-20 h-20 bg-indigo-600 rounded-[2rem] flex items-center justify-center text-white mx-auto shadow-xl"><Heart size={40} fill="currentColor"/></div>
          <h1 className="text-4xl font-black text-gray-800 tracking-tighter">{isLogin ? '欢迎回来' : '开启健康之旅'}</h1>
          <p className="text-gray-400 font-bold">{isLogin ? '登录以继续管理你的健康' : '立即注册，量化你的蜕变'}</p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          {error && <div className="p-4 bg-red-50 text-red-500 rounded-2xl text-xs font-bold border border-red-100">{error}</div>}
          <div className="relative group">
            <User className="absolute left-5 top-1/2 -translate-y-1/2 text-gray-300 group-focus-within:text-indigo-500 transition" size={20}/>
            <input required className="w-full pl-14 pr-5 py-5 bg-gray-50 rounded-2xl border-none font-bold text-gray-700 focus:ring-2 focus:ring-indigo-100 outline-none transition" placeholder="用户名" value={formData.username} onChange={e=>setFormData({...formData, username:e.target.value})} />
          </div>
          <div className="relative group">
            <Lock className="absolute left-5 top-1/2 -translate-y-1/2 text-gray-300 group-focus-within:text-indigo-500 transition" size={20}/>
            <input required type="password" className="w-full pl-14 pr-5 py-5 bg-gray-50 rounded-2xl border-none font-bold text-gray-700 focus:ring-2 focus:ring-indigo-100 outline-none transition" placeholder="密码" value={formData.password} onChange={e=>setFormData({...formData, password:e.target.value})} />
          </div>
          {!isLogin && (
            <>
              <div className="relative group">
                <Edit3 className="absolute left-5 top-1/2 -translate-y-1/2 text-gray-300 group-focus-within:text-indigo-500 transition" size={20}/>
                <input required className="w-full pl-14 pr-5 py-5 bg-gray-50 rounded-2xl border-none font-bold text-gray-700 focus:ring-2 focus:ring-indigo-100 outline-none transition" placeholder="昵称" value={formData.nickname} onChange={e=>setFormData({...formData, nickname:e.target.value})} />
              </div>
              <div className="relative group">
                <Mail className="absolute left-5 top-1/2 -translate-y-1/2 text-gray-300 group-focus-within:text-indigo-500 transition" size={20}/>
                <input required type="email" className="w-full pl-14 pr-5 py-5 bg-gray-50 rounded-2xl border-none font-bold text-gray-700 focus:ring-2 focus:ring-indigo-100 outline-none transition" placeholder="电子邮箱" value={formData.email} onChange={e=>setFormData({...formData, email:e.target.value})} />
              </div>
            </>
          )}
          <button type="submit" className="w-full bg-indigo-600 text-white py-6 rounded-3xl font-black text-xl shadow-xl shadow-indigo-100 hover:bg-indigo-700 hover:scale-[1.02] active:scale-[0.98] transition-all">
            {isLogin ? '立即登录' : '创建账户'}
          </button>
        </form>

        <div className="text-center">
          <button onClick={() => setIsLogin(!isLogin)} className="text-sm font-bold text-gray-400 hover:text-indigo-600 transition flex items-center gap-2 mx-auto">
            {isLogin ? <><UserPlus size={16}/> 没有账号？点击注册</> : <><User size={16}/> 已有账号？前往登录</>}
          </button>
        </div>
      </div>
    </div>
  );
};

// --- 工具组件: 附件链接解析 ---
const AttachmentLink = ({ note }) => {
  const match = note?.match(/\[附件: (.*?)\]/);
  if (!match) return null;
  const fileName = match[1];
  return (
    <a href={`${API_BASE}/files/${fileName}`} target="_blank" rel="noreferrer" className="inline-flex items-center gap-1 text-[10px] font-black text-indigo-500 bg-indigo-50 px-2 py-1 rounded-lg hover:bg-indigo-100 transition mt-1 w-fit">
      <ExternalLink size={10} /> 查看附件
    </a>
  );
};

// --- 弹窗组件: 编辑习惯 ---
const EditHabitModal = ({ habit, onClose, onSave }) => {
  const [formData, setFormData] = useState({ ...habit, frequency: habit.frequency_unit || 'day' });
  return (
    <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-6">
      <div className="bg-white w-full max-w-lg rounded-[3rem] p-12 shadow-2xl space-y-8 animate-in fade-in zoom-in duration-300">
        <div className="flex justify-between items-center"><h3 className="text-3xl font-black text-gray-800">编辑习惯</h3><button onClick={onClose} className="p-3 bg-gray-100 rounded-2xl hover:bg-gray-200 transition"><X /></button></div>
        <div className="space-y-6">
          <div className="space-y-2"><label className="text-xs font-black text-gray-400 uppercase ml-4">习惯名称</label><input className="w-full p-5 bg-gray-50 rounded-2xl border-none font-bold text-lg" value={formData.habit_name} onChange={e=>setFormData({...formData, habit_name: e.target.value})} /></div>
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2"><label className="text-xs font-black text-gray-400 uppercase ml-4">目标周期</label><select className="w-full p-5 bg-gray-50 rounded-2xl border-none font-bold text-lg" value={formData.frequency} onChange={e=>setFormData({...formData, frequency: e.target.value})}><option value="day">每日目标</option><option value="week">每周目标</option></select></div>
            <div className="space-y-2"><label className="text-xs font-black text-gray-400 uppercase ml-4">目标数值</label><input type="number" className="w-full p-5 bg-gray-50 rounded-2xl border-none font-bold text-lg" value={formData.target_value} onChange={e=>setFormData({...formData, target_value: e.target.value})} /></div>
          </div>
          <div className="space-y-2"><label className="text-xs font-black text-gray-400 uppercase ml-4">单位</label><input className="w-full p-5 bg-gray-50 rounded-2xl border-none font-bold text-lg" value={formData.target_unit} onChange={e=>setFormData({...formData, target_unit: e.target.value})} /></div>
          <button onClick={() => onSave(formData)} className="w-full bg-indigo-600 text-white py-6 rounded-[2rem] font-black text-xl shadow-xl hover:bg-indigo-700 transition">保存修改</button>
        </div>
      </div>
    </div>
  );
};

// --- 弹窗组件: 新增习惯 ---
const AddHabitModal = ({ onClose, onSave }) => {
  const [formData, setFormData] = useState({ habitName: '', targetValue: 1, targetUnit: '次', frequency: 'day' });
  return (
    <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-6">
      <div className="bg-white w-full max-w-lg rounded-[3rem] p-12 shadow-2xl space-y-8 animate-in fade-in zoom-in duration-300">
        <div className="flex justify-between items-center"><h3 className="text-3xl font-black text-gray-800">新增计划</h3><button onClick={onClose} className="p-3 bg-gray-100 rounded-2xl hover:bg-gray-200 transition"><X /></button></div>
        <div className="space-y-6">
          <input placeholder="想养成什么习惯？" className="w-full p-6 bg-gray-50 rounded-2xl border-none font-bold text-xl" value={formData.habitName} onChange={e=>setFormData({...formData, habitName: e.target.value})} />
          <div className="grid grid-cols-2 gap-4">
            <select className="p-6 bg-gray-50 rounded-2xl border-none font-bold text-xl" value={formData.frequency} onChange={e=>setFormData({...formData, frequency: e.target.value})}><option value="day">每日目标</option><option value="week">每周目标</option></select>
            <input type="number" placeholder="目标值" className="w-full p-6 bg-gray-50 rounded-2xl border-none font-bold text-xl" value={formData.targetValue} onChange={e=>setFormData({...formData, targetValue: e.target.value})} />
          </div>
          <input placeholder="单位" className="w-full p-6 bg-gray-50 rounded-2xl border-none font-bold text-xl" value={formData.targetUnit} onChange={e=>setFormData({...formData, targetUnit: e.target.value})} />
          <button onClick={() => onSave(formData)} className="w-full bg-indigo-600 text-white py-6 rounded-[2rem] font-black text-xl shadow-xl hover:bg-indigo-700 transition">开启新计划</button>
        </div>
      </div>
    </div>
  );
};

// --- 模块 1: 健康录入 ---
const HealthSquare = ({ userId }) => {
  const [healthData, setHealthData] = useState({ weight: 65, systolic: 120, diastolic: 80 });
  const [exercise, setExercise] = useState({ type: '跑步', duration: 30, calories: 200, feeling: 'good', note: '' });
  const [history, setHistory] = useState([]);
  const [exerciseHistory, setExerciseHistory] = useState([]);
  const [punchHistory, setPunchHistory] = useState([]);

  const fetchData = () => {
    fetch(`${API_BASE}/report/health/${userId}`).then(res => res.json()).then(data => setHistory(data));
    fetch(`${API_BASE}/report/exercise/${userId}`).then(res => res.json()).then(data => setExerciseHistory(data));
    fetch(`${API_BASE}/report/punches/${userId}`).then(res => res.json()).then(data => setPunchHistory(data));
  };
  useEffect(() => { fetchData(); }, [userId]);

  const saveHealthItem = async (type, val, unit) => {
    const res = await fetch(`${API_BASE}/health/${userId}`, {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ data_type: type, data_value: parseFloat(val) || 0, data_unit: unit, record_date: new Date().toISOString().split('T')[0] })
    });
    if (!res.ok) throw new Error(`[${type}] 同步失败`);
  };

  const handleHealthSubmit = async () => {
    try {
      await saveHealthItem('weight', healthData.weight, 'kg');
      await saveHealthItem('systolic', healthData.systolic, 'mmHg');
      await saveHealthItem('diastolic', healthData.diastolic, 'mmHg');
      alert("指标已同步！"); fetchData();
    } catch (err) { alert(err.message); }
  };

  const saveExercise = async () => {
    try {
      const res = await fetch(`${API_BASE}/exercise/${userId}`, {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ type: exercise.type, duration: parseInt(exercise.duration) || 0, calories: parseFloat(exercise.calories) || 0, feeling: exercise.feeling, date: new Date().toISOString().split('T')[0], note: exercise.note })
      });
      if (!res.ok) throw new Error(`运动保存失败`);
      alert("运动记录已存入日志！"); fetchData();
    } catch (err) { alert(err.message); }
  };

  return (
    <div className="p-8 max-w-6xl mx-auto space-y-10">
      <h2 className="text-4xl font-black text-gray-800 tracking-tighter flex items-center gap-3"><Database className="text-pink-500" /> 健康与运动</h2>
      <div className="grid lg:grid-cols-2 gap-10">
        <div className="space-y-10">
          <div className="bg-white p-12 rounded-[3.5rem] shadow-sm border border-gray-100 space-y-8">
            <div className="flex items-center gap-3 text-2xl font-black"><Thermometer className="text-pink-500" /> 生理指标录入</div>
            <div className="space-y-6">
              <div className="space-y-4">
                <label className="text-sm font-black text-gray-400 uppercase flex justify-between"><span>体重: {healthData.weight} KG</span></label>
                <input type="range" min="40" max="150" step="0.1" className="w-full h-3 bg-gray-100 rounded-lg appearance-none cursor-pointer accent-pink-500" value={healthData.weight} onChange={e=>setHealthData({...healthData, weight: e.target.value})} />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div className="relative"><label className="text-[10px] font-black text-gray-400 uppercase absolute top-2 left-5">收缩压</label><input type="number" className="w-full p-5 pt-7 bg-gray-50 rounded-2xl border-none font-bold" value={healthData.systolic} onChange={e=>setHealthData({...healthData, systolic: e.target.value})} /><span className="absolute right-4 bottom-5 text-[10px] font-bold text-gray-300">mmHg</span></div>
                <div className="relative"><label className="text-[10px] font-black text-gray-400 uppercase absolute top-2 left-5">舒张压</label><input type="number" className="w-full p-5 pt-7 bg-gray-50 rounded-2xl border-none font-bold" value={healthData.diastolic} onChange={e=>setHealthData({...healthData, diastolic: e.target.value})} /><span className="absolute right-4 bottom-5 text-[10px] font-bold text-gray-300">mmHg</span></div>
              </div>
              <button onClick={handleHealthSubmit} className="w-full bg-pink-500 text-white py-6 rounded-[2rem] font-black text-xl shadow-xl shadow-pink-100 hover:bg-pink-600 transition">更新今日指标</button>
            </div>
          </div>
          <div className="bg-white p-12 rounded-[3.5rem] shadow-sm border border-gray-100 space-y-8">
            <div className="flex items-center gap-3 text-2xl font-black"><Flame className="text-orange-500" /> 运动记录打卡</div>
            <div className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <select className="p-5 bg-gray-50 rounded-2xl border-none font-bold text-lg" value={exercise.type} onChange={e=>setExercise({...exercise, type: e.target.value})}><option value="跑步">跑步</option><option value="瑜伽">瑜伽</option><option value="游泳">游泳</option><option value="骑行">骑行</option></select>
                <div className="relative"><input type="number" className="p-5 bg-gray-50 rounded-2xl border-none font-bold text-lg w-full" value={exercise.duration} onChange={e=>setExercise({...exercise, duration: e.target.value})} /><span className="absolute right-4 top-1/2 -translate-y-1/2 font-bold text-gray-300">min</span></div>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div className="relative"><label className="text-[10px] font-black text-gray-400 uppercase absolute top-2 left-5">能量消耗</label><input type="number" className="p-5 pt-7 bg-gray-50 rounded-2xl border-none font-bold text-lg w-full" value={exercise.calories} onChange={e=>setExercise({...exercise, calories: e.target.value})} /><span className="absolute right-4 bottom-5 text-[10px] font-bold text-gray-300">kcal</span></div>
                <select className="p-5 bg-gray-50 rounded-2xl border-none font-bold text-lg" value={exercise.feeling} onChange={e=>setExercise({...exercise, feeling: e.target.value})}><option value="good">很好</option><option value="normal">一般</option><option value="tired">疲惫</option></select>
              </div>
              <textarea placeholder="心得备注..." className="w-full p-5 bg-gray-50 rounded-2xl border-none font-bold h-24" value={exercise.note} onChange={e=>setExercise({...exercise, note: e.target.value})}></textarea>
              <button onClick={saveExercise} className="w-full bg-indigo-600 text-white py-6 rounded-[2rem] font-black text-xl hover:bg-indigo-700 transition">保存记录</button>
            </div>
          </div>
        </div>
        <div className="bg-white p-12 rounded-[3.5rem] shadow-sm border border-gray-100 flex flex-col h-[850px]">
          <h3 className="text-2xl font-black mb-8 flex items-center gap-3"><History className="text-indigo-600" /> 活动历史流水</h3>
          <div className="flex-1 overflow-y-auto space-y-4 pr-2 custom-scrollbar">
            {punchHistory.map((p, i) => (
              <div key={`p-${i}`} className="p-6 bg-emerald-50/50 rounded-3xl border border-emerald-100 flex justify-between items-center">
                <div className="flex items-center gap-4">
                  <div className="w-12 h-12 bg-emerald-500 rounded-2xl flex items-center justify-center text-white"><CheckCircle size={20}/></div>
                  <div><div className="font-black text-gray-800">{p.habit_name}</div><div className="text-xs font-bold text-gray-400">{p.record_date}</div><AttachmentLink note={p.note} /></div>
                </div>
                <div className="text-right font-black text-emerald-600">+{p.fact_value}</div>
              </div>
            ))}
            {exerciseHistory.map((ex, i) => (
              <div key={`ex-${i}`} className="p-6 bg-orange-50/50 rounded-3xl border border-orange-100 flex justify-between items-center group hover:bg-orange-50 transition">
                <div className="flex items-center gap-4">
                  <div className="w-12 h-12 bg-orange-500 rounded-2xl flex items-center justify-center text-white"><Flame size={20}/></div>
                  <div><div className="font-black text-gray-800">{ex.sports_type}</div><div className="text-xs font-bold text-gray-400">{ex.exercise_record_date}</div></div>
                </div>
                <div className="text-right"><div className="font-black text-orange-600">{ex.duration} min</div><div className="text-xs font-bold text-orange-300">{ex.calories} kcal</div></div>
              </div>
            ))}
            {history.map((h, i) => (
              <div key={`he-${i}`} className="p-6 bg-indigo-50/30 rounded-3xl border border-indigo-50 flex justify-between items-center">
                <div className="flex items-center gap-4">
                  <div className={`w-12 h-12 rounded-2xl flex items-center justify-center ${h.data_type === 'weight' ? 'bg-pink-500 text-white' : 'bg-indigo-500 text-white'}`}>{h.data_type === 'weight' ? <Database size={20}/> : <Thermometer size={20}/>}</div>
                  <div><div className="font-black text-gray-800">{h.data_type === 'weight' ? '体重' : h.data_type === 'systolic' ? '收缩压' : '舒张压'}</div><div className="text-xs font-bold text-gray-400">{h.record_date}</div></div>
                </div>
                <div className="text-xl font-black text-indigo-600">{h.data_value} <span className="text-[10px] text-gray-300">{h.data_unit}</span></div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};

// --- 模块 2: 个人资料 ---
const UserProfile = ({ userId, userData }) => {
  const [isEditing, setIsEditing] = useState(false);
  const [user, setUser] = useState({ nickname: userData?.nickname || '用户', email: userData?.email || '' });
  const [avatar, setAvatar] = useState(localStorage.getItem(`userAvatar_${userId}`) || 'https://ui-avatars.com/api/?name=User&background=6366f1&color=fff');
  const handleUpdate = async () => { await fetch(`${API_BASE}/user/profile/${userId}`, { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(user) }); setIsEditing(false); alert("云端已同步！"); };
  const handleAvatar = (e) => { const file = e.target.files[0]; if (file) { const reader = new FileReader(); reader.onloadend = () => { setAvatar(reader.result); localStorage.setItem(`userAvatar_${userId}`, reader.result); }; reader.readAsDataURL(file); } };
  return (
    <div className="p-8 max-w-4xl mx-auto"><div className="bg-white p-16 rounded-[4rem] shadow-2xl border border-gray-50 text-center space-y-10 relative overflow-hidden"><div className="absolute top-0 left-0 w-full h-32 bg-indigo-600"></div><div className="relative z-10"><div className="relative group w-44 h-44 mx-auto mb-10"><img src={avatar} className="w-full h-full rounded-[3.5rem] object-cover border-8 border-white shadow-2xl" alt="avatar" /><label className="absolute inset-0 bg-black/40 rounded-[3.5rem] flex items-center justify-center opacity-0 group-hover:opacity-100 transition cursor-pointer text-white"><Camera size={32} /><input type="file" className="hidden" onChange={handleAvatar} /></label></div>{isEditing ? (<div className="max-w-md mx-auto space-y-6 text-left"><input className="w-full p-5 bg-gray-50 rounded-2xl border-none font-bold" placeholder="昵称" value={user.nickname} onChange={e=>setUser({...user, nickname:e.target.value})} /><input className="w-full p-5 bg-gray-50 rounded-2xl border-none font-bold" placeholder="邮箱" value={user.email} onChange={e=>setUser({...user, email:e.target.value})} /><button onClick={handleUpdate} className="w-full bg-green-500 text-white py-5 rounded-[2rem] font-black text-xl">保存到数据库</button></div>) : (<div className="space-y-6"><h2 className="text-5xl font-black text-gray-800">{user.nickname}</h2><p className="text-gray-400 font-bold">{user.email}</p><button onClick={()=>setIsEditing(true)} className="bg-indigo-600 text-white px-12 py-5 rounded-[2rem] font-black text-xl flex items-center gap-3 mx-auto"><Edit3 /> 修改资料</button></div>)}</div></div></div>
  );
};

// --- 模块 3: 习惯管理 ---
const HabitManager = ({ userId }) => {
  const [habits, setHabits] = useState([]);
  const [editingHabit, setEditingHabit] = useState(null);
  const [isAdding, setIsAdding] = useState(false);
  const fetchHabits = () => fetch(`${API_BASE}/habits/${userId}`).then(res => res.json()).then(data => setHabits(data));
  useEffect(() => { fetchHabits(); }, [userId]);
  const toggleStatus = async (id, current) => { await fetch(`${API_BASE}/habits/${id}/status?enabled=${!current}`, { method: 'PUT' }); fetchHabits(); };
  const deleteHabit = async (id) => { if (window.confirm("确定要物理删除该习惯及其所有历史记录吗？")) { try { const res = await fetch(`${API_BASE}/habits/${id}`, { method: 'DELETE' }); if (!res.ok) throw new Error("删除失败"); setHabits(prev => prev.filter(h => h.user_habit_id !== id)); alert("已彻底删除！"); } catch (e) { alert(e.message); } } };
  const handleEditSave = async (updated) => { await fetch(`${API_BASE}/habits/${updated.user_habit_id}/settings`, { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ habitName: updated.habit_name, targetValue: updated.target_value, targetUnit: updated.target_unit, frequency: updated.frequency }) }); setEditingHabit(null); fetchHabits(); };
  const handleAddSave = async (newData) => { await fetch(`${API_BASE}/habits/${userId}`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(newData) }); setIsAdding(false); fetchHabits(); };
  return (
    <div className="p-8 max-w-5xl mx-auto space-y-10">
      <div className="flex justify-between items-center"><h2 className="text-4xl font-black text-gray-800 tracking-tighter">习惯中心</h2><button onClick={()=>setIsAdding(true)} className="bg-indigo-600 text-white px-8 py-4 rounded-2xl font-black flex items-center gap-2"><Plus /> 新增计划</button></div>
      <div className="grid gap-6">
        {habits.map(h => (
          <div key={h.user_habit_id} className={`p-8 rounded-[3rem] border transition-all duration-300 flex justify-between items-center bg-white ${h.user_habit_status ? 'border-gray-100 shadow-sm' : 'opacity-50 grayscale'}`}>
            <div className="flex items-center gap-6">
              <div className="w-16 h-16 bg-indigo-50 rounded-2xl flex items-center justify-center font-black text-2xl text-indigo-600">{h.habit_name?.charAt(0)}</div>
              <div><h3 className="font-black text-2xl text-gray-800">{h.habit_name}</h3><div className="flex gap-4 mt-1 text-sm font-bold text-gray-400"><span>目标: {h.target_value} {h.target_unit} / {h.frequency_unit === 'day' ? '日' : '周'}</span></div></div>
            </div>
            <div className="flex gap-3"><button onClick={() => setEditingHabit(h)} className="p-4 bg-gray-50 rounded-2xl hover:bg-indigo-50 text-gray-500"><Edit3 size={20}/></button><button onClick={() => toggleStatus(h.user_habit_id, h.user_habit_status)} className={`px-6 py-4 rounded-2xl font-black ${h.user_habit_status ? 'bg-green-100 text-green-600' : 'bg-gray-100 text-gray-400'}`}>{h.user_habit_status ? '正在运行' : '已暂停'}</button><button onClick={() => deleteHabit(h.user_habit_id)} className="p-4 bg-red-50 text-red-500 rounded-2xl hover:bg-red-500 hover:text-white transition"><Trash2 size={20}/></button></div>
          </div>
        ))}
      </div>
      {editingHabit && <EditHabitModal habit={editingHabit} onClose={()=>setEditingHabit(null)} onSave={handleEditSave} />}
      {isAdding && <AddHabitModal onClose={()=>setIsAdding(false)} onSave={handleAddSave} />}
    </div>
  );
};

// --- 辅助组件: 打卡卡片 ---
const PunchCard = ({ habit, userId, onPunchSuccess }) => {
  const [todayVal, setTodayVal] = useState(0);
  const [history, setHistory] = useState([]);
  const [file, setFile] = useState(null);
  const [showHistory, setShowHistory] = useState(false);
  const fetchData = async () => { const todayRes = await fetch(`${API_BASE}/habits/${habit.user_habit_id}/punch/today`).then(r=>r.json()); setTodayVal(todayRes.todayValue); const historyRes = await fetch(`${API_BASE}/habits/${habit.user_habit_id}/punch/recent`).then(r=>r.json()); setHistory(historyRes); };
  useEffect(() => { fetchData(); }, [habit.user_habit_id]);
  const handlePunch = async () => {
    try {
      let note = "今日达成";
      if (file) { const formData = new FormData(); formData.append('file', file); formData.append('userId', userId); formData.append('relateType', 'record'); formData.append('relateId', habit.user_habit_id); const uploadRes = await fetch(`${API_BASE}/upload`, { method: 'POST', body: formData }).then(r=>r.json()); note += ` [附件: ${uploadRes.filePath}]`; }
      const res = await fetch(`${API_BASE}/record/detail`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ user_habit_id: habit.user_habit_id, record_date: new Date().toISOString().split('T')[0], fact_value: habit.target_value, note }) });
      if (!res.ok) throw new Error("打卡失败"); alert(`【${habit.habit_name}】打卡成功！`); setFile(null); fetchData(); if (onPunchSuccess) onPunchSuccess();
    } catch (e) { alert(e.message); }
  };
  return (
    <div className="bg-white p-10 rounded-[3.5rem] border border-gray-100 shadow-sm hover:shadow-2xl transition-all space-y-6"><div className="flex justify-between items-start"><div><h3 className="font-black text-3xl mb-1">{habit.habit_name}</h3><p className="text-indigo-500 font-bold">目标: {habit.target_value} {habit.target_unit}</p></div><button onClick={() => setShowHistory(!showHistory)} className="p-3 bg-gray-50 rounded-2xl hover:bg-indigo-50 text-indigo-600 transition"><History size={20} /></button></div><div className="bg-gray-50 p-6 rounded-[2rem] space-y-3"><div className="flex justify-between text-xs font-black text-gray-400 uppercase"><span>今日进度</span><span>{todayVal} / {habit.target_value} {habit.target_unit}</span></div><div className="w-full bg-gray-200 rounded-full h-3 overflow-hidden"><div className="bg-indigo-600 h-full transition-all duration-1000" style={{width: `${Math.min((todayVal/habit.target_value)*100, 100)}%`}}></div></div></div>{showHistory ? (<div className="space-y-3 animate-in fade-in slide-in-from-top-4"><h4 className="text-xs font-black text-gray-400 uppercase ml-2">最近记录</h4>{history.length > 0 ? history.map((rec, i) => (<div key={i} className="flex justify-between items-center p-4 bg-gray-50 rounded-2xl text-sm"><div className="flex flex-col"><span className="font-bold text-gray-600">{rec.record_date}</span><AttachmentLink note={rec.note} /></div><span className="font-black text-indigo-600">+{rec.fact_value}</span></div>)) : <div className="text-center py-4 text-gray-300 font-bold">暂无</div>}<button onClick={() => setShowHistory(false)} className="w-full text-xs font-black text-indigo-600 uppercase py-2">收起</button></div>) : (<div className="space-y-4"><div className="flex items-center gap-3 p-4 bg-gray-50 rounded-2xl border-2 border-dashed border-gray-200 relative"><Paperclip size={20} className="text-gray-400" /><span className="text-sm font-bold text-gray-400 truncate">{file ? file.name : "凭证附件"}</span><input type="file" className="absolute inset-0 opacity-0 cursor-pointer" onChange={e => setFile(e.target.files[0])} /></div><button onClick={handlePunch} className="w-full bg-green-500 text-white font-black py-5 rounded-[2rem] shadow-lg hover:bg-green-600 transition text-lg flex items-center justify-center gap-2"><CheckCircle size={20}/> 完成打卡</button></div>)}</div>
  );
};

// --- 模块 4: 打卡中心 ---
const PunchHall = ({ userId }) => {
  const [habits, setHabits] = useState([]);
  useEffect(() => { fetch(`${API_BASE}/habits/${userId}`).then(res => res.json()).then(data => setHabits(data)); }, [userId]);
  return (<div className="p-8 max-w-6xl mx-auto space-y-10"><h2 className="text-4xl font-black text-gray-800 tracking-tighter">今日打卡中心</h2><div className="grid md:grid-cols-2 lg:grid-cols-3 gap-8">{habits.filter(h => h.user_habit_status).map(h => (<PunchCard key={h.user_habit_id} habit={h} userId={userId} />))}</div></div>);
};

const Layout = ({ children, logout, nickname }) => (
  <div className="flex min-h-screen bg-[#fafafa]"><aside className="w-80 bg-white border-r p-8 flex flex-col h-screen sticky top-0 shadow-2xl"><div className="flex items-center gap-4 mb-16"><div className="w-12 h-12 bg-indigo-600 rounded-2xl flex items-center justify-center text-white shadow-lg"><Activity/></div><span className="text-2xl font-black tracking-tighter">HEALTH PRO</span></div><div className="mb-10 px-4"><p className="text-xs font-black text-gray-400 uppercase tracking-widest">当前用户</p><p className="text-xl font-black text-gray-800">{nickname}</p></div><nav className="flex-1 space-y-2"><NavItem to="/" icon={<LayoutDashboard/>} label="仪表盘" /><NavItem to="/punch" icon={<CheckSquare/>} label="打卡中心" /><NavItem to="/health" icon={<Database/>} label="健康录入" /><NavItem to="/habits" icon={<Settings/>} label="习惯设置" /><NavItem to="/profile" icon={<UserCircle/>} label="个人中心" /></nav><button onClick={logout} className="mt-auto flex items-center gap-4 p-6 text-red-500 hover:bg-red-50 rounded-3xl transition font-black"><LogOut/> 退出系统</button></aside><main className="flex-1 overflow-auto">{children}</main></div>
);

const NavItem = ({ to, icon, label }) => (<Link to={to} className="flex items-center gap-4 p-5 text-gray-500 hover:bg-indigo-50 hover:text-indigo-600 rounded-2xl transition font-black group"><span className="group-hover:scale-110 transition">{icon}</span> {label}</Link>);

const App = () => {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [userId, setUserId] = useState(null);
  const [nickname, setNickname] = useState('');
  const [userData, setUserData] = useState(null);

  useEffect(() => {
    const savedUser = localStorage.getItem('health_user');
    if (savedUser) {
      const user = JSON.parse(savedUser);
      setIsAuthenticated(true);
      setUserId(user.id);
      setNickname(user.nickname);
      setUserData(user);
    }
  }, []);

  const logout = () => {
    localStorage.removeItem('health_user');
    setIsAuthenticated(false);
    setUserId(null);
  };

  return (
    <Router>
      {!isAuthenticated ? (
        <Auth setAuth={setIsAuthenticated} setUserId={setUserId} setNickname={setNickname} />
      ) : (
        <Layout logout={logout} nickname={nickname}>
          <Routes>
            <Route path="/" element={<Dashboard userId={userId} />} />
            <Route path="/punch" element={<PunchHall userId={userId} />} />
            <Route path="/health" element={<HealthSquare userId={userId} />} />
            <Route path="/habits" element={<HabitManager userId={userId} />} />
            <Route path="/profile" element={<UserProfile userId={userId} userData={userData} />} />
            <Route path="*" element={<Navigate to="/" />} />
          </Routes>
        </Layout>
      )}
    </Router>
  );
};

export default App;
