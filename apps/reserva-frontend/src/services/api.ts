// src/services/api.ts

import axios from 'axios';

// Criamos uma instância base do Axios com a URL do nosso backend
const baseURL = import.meta.env.VITE_API_BASE_URL;
if (!baseURL) {
  throw new Error('VITE_API_BASE_URL não definida');
}
const api = axios.create({
  baseURL,
});

// AQUI ESTÁ A MÁGICA: O Interceptor de Requisições
api.interceptors.request.use(
  (config) => {
    // 1. Antes de cada requisição sair, pegamos o token do sessionStorage
    const token = sessionStorage.getItem('authToken');

    // 2. Se o token existir, nós o adicionamos ao cabeçalho 'Authorization'
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    // 3. Retornamos a configuração da requisição para que ela continue
    return config;
  },
  (error) => {
    // Se ocorrer um erro na configuração, rejeitamos a promessa
    return Promise.reject(error);
  }
);

export default api;
 
