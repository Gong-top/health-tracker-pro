import React, { useState, useEffect } from 'react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, BarChart, Bar, Cell } from 'recharts';
import { Activity, TrendingUp, Calendar, Flame, Clock, Database } from 'lucide-react';

const Dashboard = ({ userId }) => {
  const [weeklyReport, setWeeklyReport] = useState([]);
  const [trendData, setTrendData] = useState([]);
  const [exerciseSummary, setExerciseSummary] = useState({ total_duration: 0, total_calories: 0 });
// 核心配置：腾讯云后端 API 地址 (包含 /api)
const API_BASE = "https://health-tracker-p-231843-7-1410145219.sh.run.tcloudbase.com/api";

  useEffect(() => {
    const timestamp = new Date().getTime();
    // 自动通过 API_BASE 访问，不带多余前缀
    fetch(`${API_BASE}/report/weekly/${userId}?t=${timestamp}`)
      .then(res => res.json())
      .then(data => setWeeklyReport(data));

    fetch(`${API_BASE}/report/trend/${userId}?t=${timestamp}`)
      .then(res => res.json())
      .then(data => setTrendData(data));

    fetch(`${API_BASE}/report/exercise/summary/${userId}?t=${timestamp}`)
      .then(res => res.json())
      .then(data => {
        if (data && data.length > 0) {
          setExerciseSummary(data[0]);
        }
      });
  }, [userId]);

  return (
    <div className="p-8 bg-gray-50 min-h-screen space-y-8">
      <header className="flex justify-between items-center">
        <div>
          <h1 className="text-3xl font-black text-gray-800">健康仪表盘</h1>
          <p className="text-gray-400 mt-1">监控你的习惯与运动表现</p>
        </div>
        <div className="flex gap-4">
          <button 
            onClick={() => window.open(`${API_BASE}/report/export/${userId}/${new Date().toISOString().slice(0,7)}`)}
            className="bg-white px-6 py-4 rounded-2xl shadow-sm border font-bold text-gray-600 hover:bg-gray-50 transition flex items-center gap-2"
          >
            <Database size={18} className="text-indigo-600"/> 导出月报 (CSV)
          </button>
          <div className="bg-white p-4 rounded-2xl shadow-sm border flex items-center gap-4">
            <Calendar className="text-indigo-600" />
            <span className="font-bold text-gray-700">{new Date().toLocaleDateString()}</span>
          </div>
        </div>
      </header>

      {/* 核心概览卡片 */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <div className="bg-indigo-600 p-8 rounded-[2.5rem] text-white shadow-xl shadow-indigo-100 flex flex-col justify-between h-48">
          <div className="flex justify-between items-start">
            <span className="font-bold opacity-80 uppercase tracking-widest text-xs">习惯平均完成率</span>
            <Activity size={20} />
          </div>
          <div className="text-4xl font-black">
            {weeklyReport.length > 0 
              ? Math.round(weeklyReport.reduce((acc, curr) => acc + curr.completion_rate_num, 0) / weeklyReport.length) 
              : 0}%
          </div>
        </div>
        <div className="bg-orange-500 p-8 rounded-[2.5rem] text-white shadow-xl shadow-orange-100 flex flex-col justify-between h-48">
          <div className="flex justify-between items-start">
            <span className="font-bold opacity-80 uppercase tracking-widest text-xs">本周运动消耗</span>
            <Flame size={20} />
          </div>
          <div className="text-4xl font-black">{Math.round(exerciseSummary.total_calories)} <span className="text-sm">kcal</span></div>
        </div>
        <div className="bg-emerald-500 p-8 rounded-[2.5rem] text-white shadow-xl shadow-emerald-100 flex flex-col justify-between h-48">
          <div className="flex justify-between items-start">
            <span className="font-bold opacity-80 uppercase tracking-widest text-xs">本周锻炼时长</span>
            <Clock size={20} />
          </div>
          <div className="text-4xl font-black">{exerciseSummary.total_duration} <span className="text-sm">min</span></div>
        </div>
        <div className="bg-white p-8 rounded-[2.5rem] border border-gray-100 flex flex-col justify-between h-48 shadow-sm">
           <div className="flex justify-between items-start">
            <span className="font-bold text-gray-400 uppercase tracking-widest text-xs">正在进行习惯</span>
            <TrendingUp size={20} className="text-indigo-600"/>
          </div>
          <div className="text-4xl font-black text-gray-800">{weeklyReport.length} <span className="text-sm">个</span></div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <div className="lg:col-span-2 bg-white p-8 rounded-3xl shadow-sm border">
          <h2 className="text-xl font-bold mb-6 flex items-center gap-2">
            <TrendingUp className="text-green-500" /> 近 7 天打卡总量趋势
          </h2>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={trendData}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f0f0f0" />
                <XAxis dataKey="record_date" axisLine={false} tickLine={false} tick={{fill: '#9ca3af', fontSize: 12}} />
                <YAxis axisLine={false} tickLine={false} tick={{fill: '#9ca3af', fontSize: 12}} />
                <Tooltip contentStyle={{borderRadius: '16px', border: 'none', boxShadow: '0 10px 15px -3px rgb(0 0 0 / 0.1)'}} />
                <Line type="monotone" dataKey="daily_total" stroke="#6366f1" strokeWidth={4} dot={{r: 6, fill: '#6366f1', strokeWidth: 2, stroke: '#fff'}} activeDot={{r: 8}} />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </div>

        <div className="bg-white p-8 rounded-3xl shadow-sm border">
          <h2 className="text-xl font-bold mb-6 flex items-center gap-2">
            <Activity className="text-indigo-500" /> 习惯完成率对比
          </h2>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={weeklyReport}>
                <XAxis dataKey="fact_name" hide />
                <Tooltip />
                <Bar dataKey="completion_rate_num" radius={[10, 10, 10, 10]}>
                  {weeklyReport.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={entry.completion_rate_num >= 100 ? '#10b981' : '#6366f1'} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {weeklyReport.map(habit => (
          <div key={habit.user_habit_id} className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 relative overflow-hidden group">
            <div className={`absolute top-0 left-0 w-2 h-full ${habit.completion_rate_num >= 100 ? 'bg-green-500' : 'bg-indigo-500'}`}></div>
            <div className="flex justify-between items-start mb-4">
              <h3 className="font-bold text-lg text-gray-800">{habit.fact_name}</h3>
              <span className="text-xs font-bold px-2 py-1 bg-gray-100 rounded-lg text-gray-500 uppercase tracking-wider">{habit.target_unit}</span>
            </div>
            <div className="space-y-3">
              <div className="flex justify-between text-sm">
                <span className="text-gray-400">已完成: {habit.total_finished}</span>
                <span className="text-gray-400">周目标: {habit.weekly_target}</span>
              </div>
              <div className="w-full bg-gray-100 rounded-full h-3">
                <div 
                  className={`h-3 rounded-full transition-all duration-1000 ${habit.completion_rate_num >= 100 ? 'bg-green-500' : 'bg-indigo-600'}`}
                  style={{ width: `${Math.min(habit.completion_rate_num, 100)}%` }}
                ></div>
              </div>
              <p className="text-right text-sm font-black text-gray-700">{habit.completion_rate_num}% 完成度</p>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default Dashboard;
