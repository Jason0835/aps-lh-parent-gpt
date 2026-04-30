<template>
  <!-- 授权用户 -->
  <el-dialog
    :title="$t('common.api.role.title.selectUser')"
    :visible.sync="visible"
    width="800px"
    top="5vh"
    append-to-body
    :close-on-click-modal="false"
    :close-on-press-escape="false"
  >
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true">
      <el-form-item
        :label="$t('common.api.user.columnname.username')"
        prop="userName"
      >
        <el-input
          v-model="queryParams.userName"
          :placeholder="$t('common.api.user.placeholder.userName')"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item
        :label="$t('common.api.user.columnname.telphone')"
        prop="phonenumber"
      >
        <el-input
          v-model="queryParams.phonenumber"
          :placeholder="$t('common.api.user.placeholder.phoneNumber')"
          clearable
          @keyup.enter.native="handleQuery"
        />
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
    <el-row>
      <t-table
        @row-click="clickRow"
        v-loading="loading"
        ref="table"
        :data="userList"
        @selection-change="handleSelectionChange"
        height="260px"
        border
        :empty-text="this.$t('common.emptyDataDescription')"
        :sum-text="this.$t('common.sum')"
      >
        <t-table-column type="selection" width="55"></t-table-column>
        <t-table-column
          :label="$t('common.api.user.columnname.username')"
          prop="userName"
          :show-overflow-tooltip="true"
        />
        <t-table-column
          :label="$t('common.api.user.columnname.nickname')"
          prop="nickName"
          :show-overflow-tooltip="true"
        />
        <t-table-column
          :label="$t('common.api.user.columnname.email')"
          prop="email"
          :show-overflow-tooltip="true"
        />
        <t-table-column
          :label="$t('common.api.user.columnname.telphone')"
          prop="phonenumber"
          :show-overflow-tooltip="true"
        />
        <t-table-column
          :label="$t('common.status')"
          align="center"
          prop="status"
        >
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
          width="180"
        >
          <template slot-scope="scope">
            <span>{{ parseTime(scope.row.createTime) }}</span>
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
    </el-row>
    <div slot="footer" class="dialog-footer">
      <el-button type="primary" :loading="loading" @click="handleSelectUser">{{
        $t("common.button.confirm")
      }}</el-button>
      <el-button :loading="loading" @click="visible = false">{{
        $t("common.button.cancel")
      }}</el-button>
    </div>
  </el-dialog>
</template>

<script>
import { unallocatedUserList, authUserSelectAll } from "@/api/system/role";
export default {
  dicts: ["sys_normal_disable"],
  props: {
    // 角色编号
    roleId: {
      type: [Number, String],
    },
  },
  data() {
    return {
      // 遮罩层
      visible: false,
      loading: false,
      // 选中数组值
      userIds: [],
      // 总条数
      total: 0,
      // 未授权用户数据
      userList: [],
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        roleId: undefined,
        userName: undefined,
        phonenumber: undefined,
      },
    };
  },
  methods: {
    // 显示弹框
    show() {
      this.queryParams.roleId = this.roleId;
      this.getList();
      this.visible = true;
    },
    clickRow(row) {
      this.$refs.table.toggleRowSelection(row);
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.userIds = selection.map((item) => item.userId);
    },
    // 查询表数据
    async getList() {
      try {
        this.loading = true;
        const res = await unallocatedUserList(this.queryParams);
        this.userList = res.rows;
        this.total = res.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
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
    /** 选择授权用户操作 */
    async handleSelectUser() {
      const roleId = this.queryParams.roleId;
      const userIds = this.userIds.join(",");
      if (userIds == "") {
        this.$modal.msgError(this.$t("common.api.role.msg.noSelectUserError"));
        return;
      }
      try {
        this.loading = true;
        const res = await authUserSelectAll({
          roleId: roleId,
          userIds: userIds,
        });
        this.$modal.msgSuccess(res.msg);
        if (res.code === 200) {
          this.visible = false;
          this.$emit("ok");
        }
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
      // authUserSelectAll({ roleId: roleId, userIds: userIds }).then(res => {
      //   this.$modal.msgSuccess(res.msg);
      //   if (res.code === 200) {
      //     this.visible = false;
      //     this.$emit("ok");
      //   }
      // });
    },
  },
};
</script>
