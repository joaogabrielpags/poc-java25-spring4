// T5.2 (RNF-04): teste de carga de POST /api/v1/users com k6.
//
// Dois cenários, selecionados via variável de ambiente SCENARIO:
//   - "healthy" (default): ViaCEP saudável (WireMock respondendo 200 rapidamente).
//     Threshold: p(99) < 800ms.
//   - "circuit-open": circuito "viaCep" forçado aberto (WireMock respondendo 5xx
//     até a janela de 10 chamadas abrir o circuito). Threshold: p(99) < 300ms.
//
// Uso (ver perf/README.md para o setup completo de WireMock + bootRun):
//   k6 run perf/k6/register-user.js
//   k6 run -e SCENARIO=circuit-open -e BASE_URL=http://localhost:8080 perf/k6/register-user.js
import http from 'k6/http';
import { check } from 'k6';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const SCENARIO = __ENV.SCENARIO || 'healthy';

const P99_THRESHOLD_MS = SCENARIO === 'circuit-open' ? 300 : 800;

export const options = {
	vus: Number(__ENV.VUS || 20),
	duration: __ENV.DURATION || '30s',
	thresholds: {
		http_req_duration: [`p(99)<${P99_THRESHOLD_MS}`],
		http_req_failed: ['rate<0.01'],
	},
};

export default function registerUser() {
	const payload = JSON.stringify({
		name: 'Usuário k6',
		email: `k6.${uuidv4()}@example.com`,
		cep: '70040010',
	});

	const response = http.post(`${BASE_URL}/api/v1/users`, payload, {
		headers: { 'Content-Type': 'application/json' },
		tags: { scenario: SCENARIO },
	});

	check(response, {
		'status é 201': (r) => r.status === 201,
	});
}
