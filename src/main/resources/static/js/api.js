const API = {
    baseUrl: '/api',

    async request(url, options = {}) {
        const defaultOptions = {
            headers: {
                'Content-Type': 'application/json'
            }
        };

        try {
            const response = await fetch(this.baseUrl + url, { 
                ...defaultOptions, 
                ...options 
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('API请求错误:', error);
            throw error;
        }
    },

    certificates: {
        async getList() {
            return API.request('/certificates');
        },

        async getById(id) {
            return API.request(`/certificates/${id}`);
        },

        async apply(data) {
            return API.request('/certificates', {
                method: 'POST',
                body: JSON.stringify(data)
            });
        }
    }
}; 