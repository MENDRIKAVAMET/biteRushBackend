import React, { useState, useEffect } from 'react';
import { apiClient } from '../../services/api';
import { useApp } from '../../contexts/AppContext';
import './AdminDashboard.css';

interface ChartData {
  date: string;
  count: number;
  revenue: number;
}

export const AdminCharts: React.FC = () => {
  const { addError } = useApp();
  const [chartData, setChartData] = useState<ChartData[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadChartData();
  }, []);

  const loadChartData = async () => {
    try {
      setLoading(true);
      const data = await apiClient.getAdminOrdersChart();
      setChartData(data);
    } catch (err) {
      addError('Impossible de charger les graphiques', 'error');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return <div className="loading">Chargement des graphiques...</div>;
  }

  if (chartData.length === 0) {
    return <div className="error">Aucune donnée disponible</div>;
  }

  // Find max values for scaling
  const maxCount = Math.max(...chartData.map(d => d.count));
  const maxRevenue = Math.max(...chartData.map(d => d.revenue));

  return (
    <div className="admin-charts">
      <h1 className="fas fa-graphique">Graphiques & Statistiques</h1>

      <div className="charts-container">
        <div className="chart-section">
          <h2>Commandes par jour</h2>
          <div className="bar-chart">
            {chartData.map((item, idx) => (
              <div key={idx} className="bar-item">
                <div className="bar-value">{item.count}</div>
                <div 
                  className="bar"
                  style={{ height: `${(item.count / maxCount) * 200}px` }}
                  title={`${item.date}: ${item.count} commandes`}
                ></div>
                <div className="bar-label">{item.date}</div>
              </div>
            ))}
          </div>
        </div>

        <div className="chart-section">
          <h2>Revenus par jour</h2>
          <div className="bar-chart">
            {chartData.map((item, idx) => (
              <div key={idx} className="bar-item">
                <div className="bar-value">{item.revenue.toFixed(0)}</div>
                <div 
                  className="bar revenue"
                  style={{ height: `${(item.revenue / maxRevenue) * 200}px` }}
                  title={`${item.date}: ${item.revenue.toFixed(0)} Ar`}
                ></div>
                <div className="bar-label">{item.date}</div>
              </div>
            ))}
          </div>
        </div>
      </div>

      <div className="chart-table">
        <h2>Détails journaliers</h2>
        <table>
          <thead>
            <tr>
              <th>Date</th>
              <th>Commandes</th>
              <th>Revenu (Ar)</th>
              <th>Revenu moyen</th>
            </tr>
          </thead>
          <tbody>
            {chartData.map((item, idx) => (
              <tr key={idx}>
                <td>{item.date}</td>
                <td className="text-center">{item.count}</td>
                <td className="text-right">{item.revenue.toFixed(2)}</td>
                <td className="text-right">{(item.revenue / item.count).toFixed(2)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="quick-actions">
        <button onClick={loadChartData} className="btn btn-primary">
          Actualiser
        </button>
      </div>
    </div>
  );
};
