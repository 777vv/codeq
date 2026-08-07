// codeq 前端类型（对齐 feature 02 REST API）。

export type Verdict = 'GREEN' | 'RED' | 'YELLOW' | 'PARTIAL';

export interface MethodKey {
  className: string;
  signature: string;
  route?: string;
}

export interface Totals {
  green: number;
  red: number;
  yellow: number;
  partial: number;
}

export interface ScanForm {
  repo: string;
  baseline: string;
  release: string;
  jacocoHost?: string;
  jacocoPort?: number;
  coverageXmlPath?: string;
  taskId?: string;
}

export type ScanStatusEnum = 'PENDING' | 'RUNNING' | 'SUCCESS' | 'FAILED';

export interface ScanStatus {
  taskId: string;
  status: ScanStatusEnum;
  repo: string;
  baseline: string;
  release: string;
  createdAt: string;
  startedAt?: string;
  finishedAt?: string;
  errorMsg?: string;
}

export interface ChangeItem {
  file: string;
  methodKey?: MethodKey;
  verdict: Verdict;
  uncoveredLines?: number[];
}

export interface ResultView {
  taskId: string;
  pass: boolean;
  totals: Totals;
  changes: ChangeItem[];
}
