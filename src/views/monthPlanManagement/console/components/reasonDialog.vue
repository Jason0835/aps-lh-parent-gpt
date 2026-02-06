<template>
  <el-dialog
    title="失败原因"
    :visible="visible"
    width="1000px"
    @close="hide"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :append-to-body="true"
  >
    <div style="height: 600px">
      <el-table :data="tableData" style="width: 100%"  v-loading="loading" border>
        <el-table-column prop="checkItem" :label="this.$t('ui.data.checkDialog.itemName')" show-overflow-tooltip width="240">
          <template v-slot="scope">
            <div>
              <span >{{ selectDictLabel(parentDict.type.check_item_type, scope.row.checkItem) }}</span>
            </div>
          </template>
        </el-table-column>
        </el-table-column>
        <el-table-column prop="checkContent" :label="this.$t('ui.data.checkDialog.reason')" show-overflow-tooltip >
          <template v-slot="scope">
            <div  class="error-message" v-html="scope.row.checkContent"></div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <template slot="footer">
      <el-button @click="hide">{{ this.$t("common.button.cancel") }}</el-button>
      <el-button type="primary" :loading="loading" @click="hide">{{
        this.$t("common.button.confirm")
      }}</el-button>
    </template>
  </el-dialog>
</template>

<script>
import moment from "moment";
import { checkReason } from "@/api/factory/console";

import infoForm from "@/views/components/infoForm.vue";

export default {
  components: { infoForm },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      actionData: {},
      tableData: [],
    };
  },
  computed: {},
  methods: {
    //utils
    show(data) {
      this.visible = true;
      this.actionData = data;
      this.getReasonList(data);
    },
    async getReasonList(data) {
      this.loading = true;
      try {
        let res = await checkReason(data);
        this.tableData = res.rows;
      } catch (err) {
        console.log(err);
      } finally {
        this.loading = false;
      }
    },
    hide() {
      this.visible = false;
    },

    handleConfirm() {},
  },
};
</script>
