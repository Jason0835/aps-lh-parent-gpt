<template>
  <el-dialog
    title="关联用户"
    :visible="visible"
    width="900px"
    @close="hide"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :append-to-body="true"
  >
  <div style="height:400px;">
    <page-table
      :calcHeight="true"
      tableRef="cxFixedMachineMainTable"
      v-loading="loading"
      :columns="columns"
      :searchColumns="searchColumns"
      :data="data"
      :page="page"
      :search="search"
      @refresh="getList"
      @search="handleSearch"
      @pageChange="handlePageChange"
      @sort-change="handleSortChange"
      @selection-change="handleSelectionChange"
      :showSummary="false"
      :selectArea="false"
      :toolbar="false"
    >
    </page-table>
  </div>

    <template slot="footer">
      <el-button @click="hide">{{ this.$t("common.button.cancel") }}</el-button>
      <el-button type="primary" :loading="loading" @click="handleConfirm">{{
        this.$t("common.button.confirm")
      }}</el-button>
    </template>
  </el-dialog>
</template>

<script>


import {  bindUserTemplate } from "@/api/newPage/messageTemplate";
import {listUser,} from "@/api/system/user";
//components

export default {


  provide() {
    return {
      parentDict: this.dict,
    };
  },
  data() {
    return {
      loading: false,
      visible: false,
      data: [],
      selection: [],
      page: {
        current: 1,
        pageSize: 20,
        total: 0,
      },
      sort: {},
      search: {},
      query: {},
      importDefaultValue: {},
      importRules: {},
      actionData:{}
    };
  },
  computed: {

    columns() {
      let columns = [
        { type: "selection", fixed: "left" },

        {
          prop: "userName",
          label: this.$t("common.api.user.columnname.username"),
        },

        {
          prop: "nickName",
          label: this.$t("common.api.logininfo.columnname.userName"),
        },
        // {
        //   prop: "deptName",
        //   label: this.$t("部门名称"),
        // },
        {
          prop: "phonenumber",
          label: this.$t("common.api.user.columnname.telphone"),
        },
      ];

      return columns;
    },
    searchColumns() {
      return [
        {
          prop: "userName",
          label: this.$t("common.api.user.columnname.username"),
        },

        {
          prop: "nickName",
          label: this.$t("common.api.logininfo.columnname.userName"),
        },

        {
          prop: "phonenumber",
          label: this.$t("common.api.user.columnname.telphone"),
        },
      ];
    },
  },
  methods: {
    show(data) {

      let arr=data.userName.split(',')
      console.log(arr)
      this.visible = true;
      this.actionData=data
      this.getList()
    },
    hide() {
      this.visible = false;
    },
    handleSearch(data) {
      this.query = data;
      this.$set(this.page, "current", 1);
      this.getList();
    },
    handlePageChange(current, pageSize) {
      this.$set(this.page, "current", current);
      this.$set(this.page, "pageSize", pageSize);
      this.getList();
    },

    handleSortChange({ column, prop, order }) {
      if (order) {
        this.sort = {
          orderByColumn: prop,
          isAsc: order == "ascending" ? "asc" : "desc",
        };
      } else {
        //默认排序
        this.sort = {};
      }
      this.getList();
    },
    handleSelectionChange(rows) {
      this.selection = rows;
    },

    formatParams(hasPage = true) {
      const params = {
        ...this.query,
        ...this.sort,
      };

      if (hasPage) {
        params.pageSize = this.page.pageSize;
        params.pageNum = this.page.current;
      }

      if (params.createTime && params.createTime[0]) {
        params.beginDate = params.createTime[0];
        params.endDate = params.createTime[1];
        params.createTime = undefined;
      }

      return params;
    },
    async handleConfirm() {
      if(this.selection.length==0){
        return this.$modal.msgWarning('请先选择用户');
      }
      let params={
        ...this.actionData
      }
      try {
        this.loading = true;
        let userList=''
        for (let i = 0; i < this.selection.length; i++) {
         if(i==this.selection.length-1){
          userList+=this.selection[i].userName
         }else{
          userList+=this.selection[i].userName+','
         }

        }
        params.userName=userList
        const res = await bindUserTemplate(params);
        this.$modal.msgSuccess(res.msg);
        this.$emit("success");
        this.hide();

        this.loading = false;
      } catch (error) {
        console.log(error);
        this.loading = false;
      }
    },
    // api
    async getList() {
      try {
        this.loading = true;
        const data = await listUser(this.formatParams());
        this.data = data.rows;
        this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
  },
};
</script>