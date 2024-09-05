<template>
  <div class="app-container">
    <el-form
      :model="queryParams"
      ref="queryForm"
      size="small"
      :inline="true"
      v-show="showSearch"
    >
      <el-form-item
        :label="$t('common.api.dept.columnname.deptName')"
        prop="deptName"
      >
        <el-input
          v-model="queryParams.deptName"
          :placeholder="$t('common.api.dept.placeholder.deptName')"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item :label="$t('common.status')" prop="status">
        <el-select
          v-model="queryParams.status"
          :placeholder="$t('common.api.dept.columnname.deptStatus')"
          clearable
        >
          <el-option
            v-for="dict in dict.type.sys_normal_disable"
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
          :loading="loading"
          size="mini"
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
          v-hasPermi="['system:dept:add']"
          >{{ $t("common.button.add") }}</el-button
        >
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="info"
          plain
          icon="el-icon-sort"
          size="mini"
          @click="toggleExpandAll"
          >{{ $t("common.button.expandCollapse") }}</el-button
        >
      </el-col>
      <right-toolbar
        tableRef="deptTable"
        :showSearch.sync="showSearch"
        @queryTable="getList"
      ></right-toolbar>
    </el-row>

    <t-table
      ref="deptTable"
      height="calc(100vh - 220px)"
      v-if="refreshTable"
      v-loading="loading"
      :data="deptList"
      row-key="deptId"
      :default-expand-all="isExpandAll"
      :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
      border
      :empty-text="this.$t('common.emptyDataDescription')"
      :sum-text="this.$t('common.sum')"
    >
      <t-table-column
        prop="deptName"
        :label="$t('common.api.dept.columnname.deptName')"
        width="260"
      ></t-table-column>
      <t-table-column
        prop="deptCode"
        :label="$t('common.api.dept.columnname.deptCode')"
        width="200"
      ></t-table-column>
      <t-table-column
        prop="orderNum"
        :label="$t('common.api.dept.columnname.orderNum')"
        width="200"
      ></t-table-column>
      <t-table-column prop="status" :label="$t('common.status')" width="100">
        <template slot-scope="scope">
          <dict-tag
            :options="dict.type.sys_normal_disable"
            :value="scope.row.status"
          />
        </template>
      </t-table-column>
      <t-table-column
        :label="$t('common.createTime')"
        align="center"
        prop="createTime"
        width="200"
      >
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
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
            v-hasPermi="['system:dept:edit']"
            >{{ $t("common.button.modify") }}</el-button
          >
          <el-button
            size="mini"
            type="text"
            icon="el-icon-plus"
            @click="handleAdd(scope.row)"
            v-hasPermi="['system:dept:add']"
            >{{ $t("common.button.add") }}</el-button
          >
          <el-button
            v-if="scope.row.parentId != 0"
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['system:dept:remove']"
            >{{ $t("common.button.delete") }}</el-button
          >
        </template>
      </t-table-column>
    </t-table>
    <!-- 添加或修改部门对话框 -->
    <el-dialog
      :title="title"
      :visible.sync="open"
      width="600px"
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
          <el-col :span="24" v-if="form.parentId !== 0">
            <el-form-item
              :label="$t('common.api.dept.columnname.parentDept')"
              prop="parentId"
            >
              <treeselect
                v-model="form.parentId"
                :options="deptOptions"
                :normalizer="normalizer"
                :placeholder="$t('common.api.dept.placeholder.parentDept')"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item
              :label="$t('common.api.dept.columnname.deptName')"
              prop="deptName"
            >
              <el-input
                v-model="form.deptName"
                :placeholder="$t('common.api.dept.placeholder.deptName')"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              :label="$t('common.api.dept.columnname.deptCode')"
              prop="deptCode"
            >
              <el-input
                v-model="form.deptCode"
                :placeholder="$t('common.api.dept.placeholder.deptCode')"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              :label="$t('common.api.dept.columnname.displayOrderNum')"
              prop="orderNum"
            >
              <el-input-number
                v-model="form.orderNum"
                controls-position="right"
                :min="0"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item
              :label="$t('common.api.dept.columnname.leader')"
              prop="leader"
            >
              <el-input
                v-model="form.leader"
                :placeholder="$t('common.api.dept.placeholder.leader')"
                maxlength="20"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              :label="$t('common.api.dept.columnname.phone')"
              prop="phone"
            >
              <el-input
                v-model="form.phone"
                :placeholder="$t('common.api.dept.placeholder.phone')"
                maxlength="11"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item
              :label="$t('common.api.dept.columnname.email')"
              prop="email"
            >
              <el-input
                v-model="form.email"
                :placeholder="$t('common.api.dept.placeholder.email')"
                maxlength="50"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('common.api.dept.columnname.deptStatus')">
              <el-radio-group v-model="form.status">
                <el-radio
                  v-for="dict in dict.type.sys_normal_disable"
                  :key="dict.value"
                  :label="dict.value"
                  >{{ dict.label }}</el-radio
                >
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item
              :label="$t('common.api.dept.columnname.langPackage')"
              prop="langJson"
            >
              <el-input
                v-model="form.langJson"
                :placeholder="$t('common.api.dept.placeholder.langPackage')"
              />
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
  listDept,
  getDept,
  delDept,
  addDept,
  updateDept,
  listDeptExcludeChild,
} from "@/api/system/dept";
import Treeselect from "@riophae/vue-treeselect";
import "@riophae/vue-treeselect/dist/vue-treeselect.css";

export default {
  name: "/system/dept",
  dicts: ["sys_normal_disable"],
  components: { Treeselect },
  data() {
    return {
      // 遮罩层
      loading: true,
      dialogLoading: false,
      // 显示搜索条件
      showSearch: true,
      // 表格树数据
      deptList: [],
      // 部门树选项
      deptOptions: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 是否展开，默认全部展开
      isExpandAll: true,
      // 重新渲染表格状态
      refreshTable: true,
      // 查询参数
      queryParams: {
        deptName: undefined,
        status: undefined,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        parentId: [
          {
            required: true,
            message: this.$t("common.api.dept.error.parentDept.isnull"),
            trigger: "blur",
          },
        ],
        deptCode: [
          {
            required: true,
            message: this.$t("common.api.dept.error.deptCode.isnull"),
            trigger: "blur",
          },
        ],
        deptName: [
          {
            required: true,
            message: this.$t("common.api.dept.error.deptName.isnull"),
            trigger: "blur",
          },
        ],
        orderNum: [
          {
            required: true,
            message: this.$t("common.api.dept.error.orderNum.isnull"),
            trigger: "blur",
          },
        ],
        email: [
          {
            type: "email",
            message: this.$t("common.rule.email"),
            trigger: ["blur", "change"],
          },
        ],
        phone: [
          {
            pattern: /^1[3|4|5|6|7|8|9][0-9]\d{8}$/,
            message: this.$t("common.rule.phone"),
            trigger: "blur",
          },
        ],
      },
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询部门列表 */
    async getList() {
      try {
        this.loading = true;
        const response = await listDept(this.queryParams);
        this.deptList = this.handleTree(response, "deptId");
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
    /** 转换部门数据结构 */
    normalizer(node) {
      if (node.children && !node.children.length) {
        delete node.children;
      }
      return {
        id: node.deptId,
        label: node.deptName,
        children: node.children,
      };
    },
    // 取消按钮
    cancel() {
      this.open = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.form = {
        deptId: undefined,
        parentId: undefined,
        deptCode: undefined,
        deptName: undefined,
        orderNum: undefined,
        leader: undefined,
        phone: undefined,
        email: undefined,
        status: "0",
      };
      this.resetForm("form");
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    /** 新增按钮操作 */
    handleAdd(row) {
      this.reset();
      if (row != undefined) {
        this.form.parentId = row.deptId;
      }
      this.open = true;
      this.title = this.$t("common.api.dept.title.addDept");
      listDept().then((response) => {
        this.deptOptions = this.handleTree(response, "deptId");
      });
    },
    /** 展开/折叠操作 */
    toggleExpandAll() {
      this.refreshTable = false;
      this.isExpandAll = !this.isExpandAll;
      this.$nextTick(() => {
        this.refreshTable = true;
      });
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      getDept(row.deptId).then((response) => {
        this.form = response;
        this.open = true;
        this.title = this.$t("common.api.dept.title.modifyDept");
      });
      listDeptExcludeChild(row.deptId).then((response) => {
        this.deptOptions = this.handleTree(response, "deptId");
      });
    },
    /** 提交按钮 */
    submitForm: function () {
      this.$refs["form"].validate(async (valid) => {
        if (valid) {
          try {
            this.dialogLoading = true;
            if (this.form.deptId != undefined) {
              const response = await updateDept(this.form);
              this.$modal.msgSuccess(this.$t("common.msg.success.modify"));
            } else {
              const response = await addDept(this.form);
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
      this.$modal
        // .confirm('是否确认删除名称为"' + row.deptName + '"的数据项？')
        .confirm(
          this.$t("common.api.dept.confirm.detete", { deptName: row.deptName })
        )
        .then(function () {
          return delDept(row.deptId);
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
