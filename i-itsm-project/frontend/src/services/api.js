import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';

export const loginUser = async (email) => {
    const response = await axios.get(`${API_BASE_URL}/users?email=${email}`);
    return response.data;
};

export const fetchIncidents = async () => {
    const response = await axios.get(`${API_BASE_URL}/incidents`);
    return response.data;
};

export const createIncident = async (incidentData) => {
    const response = await axios.post(`${API_BASE_URL}/incidents`, incidentData);
    return response.data;
};
