import axios from 'axios';
import type { ResultView, ScanForm, ScanStatus, Totals } from '../types';

// codeq 前端 API 封装（复用 feature 02 REST API，FR-009）。
const http = axios.create({ baseURL: '/api' });

export async function createScan(form: ScanForm): Promise<{ taskId: string; status: string }> {
  const { data } = await http.post('/scans', form);
  return data;
}

export async function getScan(id: string): Promise<ScanStatus> {
  const { data } = await http.get(`/scans/${id}`);
  return data;
}

export async function getResult(id: string): Promise<ResultView> {
  const { data } = await http.get(`/scans/${id}/result`);
  return data;
}

export async function getVerdict(id: string): Promise<{ pass: boolean; totals: Totals }> {
  const { data } = await http.get(`/scans/${id}/verdict`);
  return data;
}
