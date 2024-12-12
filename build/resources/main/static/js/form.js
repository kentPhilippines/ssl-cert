document.addEventListener('DOMContentLoaded', function() {
    const form = document.getElementById('certificateForm');
    if (!form) return;

    form.addEventListener('submit', async function(e) {
        e.preventDefault();

        // 收集表单数据
        const formData = new FormData(this);
        const data = Object.fromEntries(formData.entries());

        try {
            // 显示加载状态
            const submitBtn = this.querySelector('[type="submit"]');
            submitBtn.disabled = true;
            submitBtn.textContent = '提交中...';

            // 发送请求
            const result = await API.certificates.apply(data);
            
            // 显示成功消息
            alert('证书申请提交成功！');
            window.location.href = '/list.html';
        } catch (error) {
            alert('提交失败：' + error.message);
        } finally {
            // 恢复按钮状态
            submitBtn.disabled = false;
            submitBtn.textContent = '提交申请';
        }
    });

    // 表单验证
    form.querySelectorAll('input[required], select[required]').forEach(field => {
        field.addEventListener('blur', function() {
            validateField(this);
        });
    });
});

function validateField(field) {
    const formGroup = field.closest('.form-group');
    const errorMessage = formGroup.querySelector('.error-message');

    if (!field.value.trim()) {
        formGroup.classList.add('error');
        errorMessage.textContent = '此字段不能为空';
        return false;
    }

    if (field.type === 'email' && !isValidEmail(field.value)) {
        formGroup.classList.add('error');
        errorMessage.textContent = '请输入有效的邮箱地址';
        return false;
    }

    formGroup.classList.remove('error');
    errorMessage.textContent = '';
    return true;
}

function isValidEmail(email) {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
} 