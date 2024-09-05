<template>
  <div class="app-container">
    <el-form
      :model="queryParams"
      ref="queryForm"
      size="small"
      :inline="true"
      v-show="showSearch"
      label-width="68px"
    >
      <el-form-item
        :label="$t('common.api.sysNotice.columnname.noticeTitle')"
        prop="noticeTitle"
      >
        <el-input
          v-model="queryParams.noticeTitle"
          :placeholder="$t('common.api.sysNotice.placeholder.noticeTitle')"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item
        :label="$t('common.api.sysNotice.columnname.operators')"
        prop="createBy"
      >
        <el-input
          v-model="queryParams.createBy"
          :placeholder="$t('common.api.sysNotice.placeholder.operators')"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item :label="$t('common.type')" prop="noticeType">
        <el-select
          v-model="queryParams.noticeType"
          :placeholder="$t('common.api.sysNotice.columnname.noticeType')"
          clearable
        >
          <el-option
            v-for="dict in dict.type.sys_notice_type"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button
          type="primary"
          icon="el-icon-search"
          size="mini"
          :loading="loading"
          @click="handleQuery"
          >{{ $t("common.button.search") }}</el-button
        >
        <el-button
          icon="el-icon-refresh"
          size="mini"
          :loading="loading"
          @click="resetQuery"
          >{{ $t("common.button.reset") }}</el-button
        >
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['system:notice:add']"
          >{{ $t("common.button.add") }}</el-button
        >
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-edit"
          size="mini"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['system:notice:edit']"
          >{{ $t("common.button.modify") }}</el-button
        >
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['system:notice:remove']"
          >{{ $t("common.button.delete") }}</el-button
        >
      </el-col>
      <right-toolbar
        tableRef="noticeTable"
        :showSearch.sync="showSearch"
        @queryTable="getList"
      ></right-toolbar>
    </el-row>

    <t-table
      ref="noticeTable"
      height="calc(100vh - 260px)"
      v-loading="loading"
      :data="noticeList"
      @selection-change="handleSelectionChange"
      border
      :empty-text="this.$t('common.emptyDataDescription')"
      :sum-text="this.$t('common.sum')"
    >
      <t-table-column type="selection" width="55" align="center" />
      <t-table-column
        :label="$t('common.api.sysNotice.columnname.noticeId')"
        align="center"
        prop="noticeId"
        width="100"
      />
      <t-table-column
        :label="$t('common.api.sysNotice.columnname.noticeTitle')"
        align="center"
        prop="noticeTitle"
        :show-overflow-tooltip="true"
      />
      <t-table-column
        :label="$t('common.api.sysNotice.columnname.noticeType')"
        align="center"
        prop="noticeType"
        width="100"
      >
        <template slot-scope="scope">
          <dict-tag
            :options="dict.type.sys_notice_type"
            :value="scope.row.noticeType"
          />
        </template>
      </t-table-column>
      <t-table-column
        :label="$t('common.status')"
        align="center"
        prop="status"
        width="100"
      >
        <template slot-scope="scope">
          <dict-tag
            :options="dict.type.sys_notice_status"
            :value="scope.row.status"
          />
        </template>
      </t-table-column>
      <t-table-column
        :label="$t('common.api.sysNotice.columnname.createBy')"
        align="center"
        prop="createBy"
        width="100"
      />
      <t-table-column
        :label="$t('common.createTime')"
        align="center"
        prop="createTime"
        width="100"
      >
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime, "{y}-{m}-{d}") }}</span>
        </template>
      </t-table-column>
      <t-table-column
        :label="$t('common.option')"
        align="center"
        class-name="small-padding fixed-width"
      >
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['system:notice:edit']"
            >{{ $t("common.button.modify") }}</el-button
          >
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['system:notice:remove']"
            >{{ $t("common.button.delete") }}</el-button
          >
        </template>
      </t-table-column>
    </t-table>

    <pagination
      v-show="total > 0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改公告对话框 -->
    <el-dialog
      :title="title"
      :visible.sync="open"
      width="780px"
      append-to-body
      :close-on-click-modal="false"
      :close-on-press-escape="false"
    >
      <el-form
        class="form-item-height"
        v-loading="dialogLoading"
        ref="form"
        :model="form"
        :rules="rules"
        label-width="80px"
      >
        <el-row>
          <el-col :span="12">
            <el-form-item
              :label="$t('common.api.sysNotice.columnname.noticeTitle')"
              prop="noticeTitle"
            >
              <el-input
                v-model="form.noticeTitle"
                :placeholder="
                  $t('common.api.sysNotice.placeholder.noticeTitle')
                "
                maxlength="150"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              :label="$t('common.api.sysNotice.columnname.noticeType')"
              prop="noticeType"
            >
              <el-select
                v-model="form.noticeType"
                :placeholder="$t('common.api.sysNotice.placeholder.noticeType')"
              >
                <el-option
                  v-for="dict in dict.type.sys_notice_type"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                ></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item :label="$t('common.status')">
              <el-radio-group v-model="form.status">
                <el-radio
                  v-for="dict in dict.type.sys_notice_status"
                  :key="dict.value"
                  :label="dict.value"
                  >{{ dict.label }}</el-radio
                >
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item
              :label="$t('common.api.sysNotice.columnname.noticeContent')"
            >
              <editor v-model="form.noticeContent" :min-height="192" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button
          type="primary"
          :loading="dialogLoading"
          @click="submitForm"
          >{{ $t("common.button.confirm") }}</el-button
        >
        <el-button :loading="dialogLoading" @click="cancel">{{
          $t("common.button.cancel")
        }}</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  listNotice,
  getNotice,
  delNotice,
  addNotice,
  updateNotice,
} from "@/api/system/notice";

export default {
  name: "/system/notice",
  dicts: ["sys_notice_status", "sys_notice_type"],
  data() {
    return {
      // 遮罩层
      loading: true,
      dialogLoading: false,
      // 选中数组
      ids: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 公告表格数据
      noticeList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        noticeTitle: undefined,
        createBy: undefined,
        status: undefined,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        noticeTitle: [
          {
            required: true,
            message: this.$t("common.api.sysNotice.error.noticeTitle.isnull"),
            trigger: "blur",
          },
        ],
        noticeType: [
          {
            required: true,
            message: this.$t("common.api.sysNotice.error.noticeType.isnull"),
            trigger: "change",
          },
        ],
      },
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询公告列表 */
    async getList() {
      try {
        this.loading = true;
        const response = await listNotice(this.queryParams);
        this.noticeList = response.rows;
        this.total = response.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
    // 取消按钮
    cancel() {
      this.open = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.form = {
        noticeId: undefined,
        noticeTitle: undefined,
        noticeType: undefined,
        noticeContent: undefined,
        status: "0",
      };
      this.resetForm("form");
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map((item) => item.noticeId);
      this.single = selection.length != 1;
      this.multiple = !selection.length;
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = this.$t("common.api.sysNotice.title.addNotice");
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const noticeId = row.noticeId || this.ids;
      getNotice(noticeId).then((response) => {
        this.form = response;
        this.open = true;
        this.title = this.$t("common.api.sysNotice.title.modifyNotice");
      });
    },
    /** 提交按钮 */
    submitForm: function () {
      this.$refs["form"].validate(async (valid) => {
        if (valid) {
          //判断富文本框长度是否超过
          if (this.form.noticeContent.length > 650) {
            this.$modal.msgError(
              this.$t("common.api.sysNotice.error.noticeContent.overlength")
            );
            return;
          }
          try {
            this.dialogLoading = true;
            if (this.form.noticeId != undefined) {
              const response = await updateNotice(this.form);
              this.$modal.msgSuccess(this.$t("common.msg.success.modify"));
            } else {
              const response = await addNotice(this.form);
              this.$modal.msgSuccess(this.$t("common.msg.success.add"));
            }
            this.open = false;
            this.getList();
          } catch (error) {
            console.error(error);
          } finally {
            this.dialogLoading = false;
          }
        }
      });
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      let noticeIds = row.noticeId || this.ids;
      if (!Array.isArray(noticeIds)) {
        noticeIds = [noticeIds];
      }
      this.$modal
        // .confirm('是否确认删除公告编号为"' + noticeIds + '"的数据项？')
        .confirm(this.$t("common.api.sysNotice.confirm.detete", { noticeIds }))
        .then(function () {
          return delNotice(noticeIds);
        })
        .then(() => {
          this.getList();
          this.$modal.msgSuccess(this.$t("common.msg.success.delete"));
        })
        .catch(() => {});
    },
  },
};
</script>
