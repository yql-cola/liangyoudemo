<template>
  <div class="login-page">
    <el-form ref="form" :model="form" :rules="rules" class="login-form" @keyup.enter.native="submit">
      <h1>粮油进销存</h1>
      <el-form-item prop="username">
        <el-input v-model="form.username" prefix-icon="el-icon-user" placeholder="用户名" />
      </el-form-item>
      <el-form-item prop="password">
        <el-input v-model="form.password" prefix-icon="el-icon-lock" placeholder="密码" show-password />
      </el-form-item>
      <el-button type="primary" :loading="loading" class="login-button" @click="submit">登录</el-button>
    </el-form>
  </div>
</template>

<script>
export default {
  name: 'Login',
  data() {
    return {
      loading: false,
      form: {
        username: '',
        password: ''
      },
      rules: {
        username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
        password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
      }
    }
  },
  methods: {
    submit() {
      this.$refs.form.validate(async valid => {
        if (!valid) return
        this.loading = true
        try {
          await this.$store.dispatch('user/login', this.form)
          const redirect = this.$route.query.redirect || '/'
          this.$router.push(redirect)
        } finally {
          this.loading = false
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #eef2f6;
}

.login-form {
  width: 360px;
  padding: 32px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.08);
}

h1 {
  margin: 0 0 24px;
  text-align: center;
  font-size: 24px;
  color: #1f2d3d;
}

.login-button {
  width: 100%;
}
</style>
