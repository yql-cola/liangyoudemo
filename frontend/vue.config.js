module.exports = {
  publicPath: '/',
  outputDir: 'dist',
  assetsDir: 'static',
  productionSourceMap: false,
  devServer: {
    port: 8081,
    watchOptions: {
      poll: 1000,
      ignored: /node_modules/
    },
    proxy: {
      [process.env.VUE_APP_BASE_API]: {
        target: process.env.VUE_APP_PROXY_TARGET || 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
}
