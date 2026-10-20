# 统一销售开单与配送交付说明

本期按 `docs/superpowers/specs/2026-10-19-unified-sales-delivery-design.md` 实施，包含旧稿要求的销售清单 PDF、Excel 导出。旧销售订单不迁移；升级前须清空旧销售测试数据，之后重新建立测试订单。

## 业务行为

- 销售单保存为草稿；确认后占用可售库存，自提交付或配送确认送达时才正式扣减总库存并写入 FIFO 批次扣减与库存流水。
- 配送未送达进入待退回，货物实际回店后释放占用。订单详情保留客户、商品、联系方式、地址、配送备注等快照和操作记录。
- 已确认订单可导出销售清单 PDF、Excel；已确认配送单另可预览、浏览器打印和下载配送 PDF。打印请求和导出操作记录操作人、时间及 `template=v1`。
- 库存汇总显示配送中占用量；销售统计按完成时间入账，待完成金额和数量单独展示。

## 数据库和字体

升级已有数据库时，先执行 `SELECT COUNT(*) FROM grain_oil_ims.biz_sale_out`；结果须为 0，否则停止，不运行本次升级脚本。本期不迁移旧销售单。之后确认结构版本：旧版库运行 `bash scripts/upgrade-v1.1.0.sh --user=<账号> --password`，再运行 `bash scripts/upgrade-unified-sales.sh --user=<账号> --password`；已是 v1.1.0 的库只运行第二个脚本。其他已有基础资料会保留。补丁是一次性 DDL，执行前备份，并先在目标 MySQL 版本的隔离副本演练。

全新空库执行顺序为 `sql/grain_oil_ims_init.sql`、`sql/grain_oil_ims_patch_2026_10_17_permissions.sql`、`sql/grain_oil_ims_patch_2026_10_19_basic_import_permissions.sql`、`sql/grain_oil_ims_seed_2026_10_20_unified_sales_permissions.sql`。init 已含本次统一销售表结构，因此新库不运行 `upgrade-v1.1.0.sh` 或 `upgrade-unified-sales.sh`。

PDF 需要可嵌入的中文 TrueType 字体。部署环境若没有程序内置候选路径的字体，配置 `SALE_PDF_FONT_PATH` 指向可用的中文 `.ttf` 文件。浏览器打印提供 A5 和可调整宽高的窄幅纸型；上线前需在门店打印机上确认纸张、边距、换页和走纸效果。

## 待现场验证

- 在目标 MySQL 版本的空销售数据副本上演练升级及旧订单拦截；本地自动化测试使用隔离 H2 数据库，不能替代该演练。
- 使用目标打印机校准纸型并检查长商品名、多页和中文字体。当前实现的 PDF 纸宽可用 `SALE_PDF_PAGE_WIDTH_POINTS` 配置。
