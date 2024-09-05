<template>
  <div class="app-container">
    <h4 class="form-header h4">{{$t('common.basicInfo')}}</h4>
    <el-form ref="form" :model="form" label-width="80px">
      <el-row>
        <el-col :span="8" :offset="2">
          <el-form-item :label="$t('common.api.user.columnname.nickname')" prop="nickName">
            <el-input v-model="form.nickName" disabled />
          </el-form-item>
        </el-col>
        <el-col :span="8" :offset="2">
          <el-form-item :label="$t('common.api.user.columnname.loginAccount')" prop="userName">
            <el-input  v-model="form.userName" disabled />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <h4 class="form-header h4">{{$t('common.api.user.columnname.roleInfo')}}</h4>
    <t-table v-loading="loading" :row-key="getRowKey" @row-click="clickRow" ref="table" @selection-change="handleSelectionChange" :data="roles.slice((pageNum-1)*pageSize,pageNum*pageSize)">
      <t-table-column :label="$t('common.api.user.columnname.index')" type="index" align="center">
        <template slot-scope="scope">
          <span>{{(pageNum - 1) * pageSize + scope.$index + 1}}</span>
        </template>
      </t-table-column>
      <t-table-column type="selection" :reserve-selection="true" width="55"></t-table-column>
      <t-table-column :label="$t('common.api.role.columnname.roleId')" align="center" prop="roleId" />
      <t-table-column :label="$t('common.api.role.columnname.name')" align="center" prop="roleName" />
      <t-table-column :label="$t('common.api.role.columnname.roleKey')" align="center" prop="roleKey" />
      <t-table-column :label="$t('common.createTime')" align="center" prop="createTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </t-table-column>
    </t-table>

    <pagination v-show="total>0" :total="total" :page.sync="pageNum" :limit.sync="pageSize" />

    <el-form label-width="100px">
      <el-form-item style="text-align: center;margin-left:-120px;margin-top:30px;">
        <el-button type="primary" :loading="loading" @click="submitForm()">{{$t('common.button.submit')}}</el-button>
        <el-button :loading="loading" @click="close()">{{$t('common.button.back')}}</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
import { getAuthRole, updateAuthRole } from "@/api/system/user";

export default {
  name: "AuthRole",
  data() {
    return {
       // 遮罩层
      loading: true,
      // 分页信息
      total: 0,
      pageNum: 1,
      pageSize: 10,
      // 选中角色编号
      roleIds:[],
      // 角色信息
      roles: [],
      // 用户信息
      form: {}
    };
  },
  created() {
    const userId = this.$route.params && this.$route.params.userId;
    if (userId) {
      this.loading = true;
      getAuthRole(userId).then((response) => {
        this.form = response.user;
        this.roles = response.roles;
        this.total = this.roles.length;
        this.$nextTick(() => {
          this.roles.forEach((row) => {
            if (row.flag) {
              this.$refs.table.toggleRowSelection(row);
            }
          });
        });
        this.loading = false;
      });
    }
  },
  methods: {
    /** 单击选中行数据 */
    clickRow(row) {
      this.$refs.table.toggleRowSelection(row);
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.roleIds = selection.map((item) => item.roleId);
    },
    // 保存选中的数据编号
    getRowKey(row) {
      return row.roleId;
    },
    /** 提交按钮 */
    async submitForm() {
      try {
        this.loading = true;
        const userId = this.form.userId;
        const roleIds = this.roleIds.join(",");
        const response = await updateAuthRole({ userId: userId, roleIds: roleIds });
        this.$modal.msgSuccess(this.$t("common.api.user.msg.authorizationSuccessful"));
        this.close();
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
    /** 关闭按钮 */
    close() {
      const obj = { path: "/system/user" };
      this.$tab.closeOpenPage(obj);
    },
  },
};
</script>
