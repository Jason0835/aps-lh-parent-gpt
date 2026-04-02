<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="600px"
    @close="hide"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :append-to-body="true"
  >
    <el-form
      ref="form"
      :model="form"
      :rules="rules"
      label-position="right"
      label-width="120px"
      v-loading="loading"
    >
      <el-row>
        <el-col :span="24">
          <el-form-item :label="$t('ui.data.column.mdmChipStock.factoryCode')" prop="factoryCode">
            <el-select
              v-model="form.factoryCode"
              :placeholder="$t('common.rule.select')"
              clearable
              filterable
              style="width: 100%"
            >
              <el-option
                v-for="item in parentDict.type.biz_factory_name"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
<!--        <el-col :span="24">-->
<!--          <el-form-item :label="$t('ui.data.column.mdmChipStock.companyCode')" prop="companyCode">-->
<!--            <el-input v-model="form.companyCode" :placeholder="$t('common.rule.input')" />-->
<!--          </el-form-item>-->
<!--        </el-col>-->
        <el-col :span="24">
          <el-form-item :label="$t('ui.data.column.mdmChipStock.chipCode')" prop="chipCode">
            <el-input v-model="form.chipCode" :placeholder="$t('common.rule.input')" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item :label="$t('ui.data.column.mdmChipStock.stockNum')" prop="stockNum">
            <el-input-number v-model="form.stockNum" :placeholder="$t('common.rule.input')" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item :label="$t('ui.data.column.mdmChipStock.finishQty')" prop="finishQty">
            <el-input-number v-model="form.finishQty" :placeholder="$t('common.rule.input')" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item :label="$t('ui.data.column.mdmChipStock.remark')" prop="remark">
            <el-input v-model="form.remark" type="textarea" :rows="3" :placeholder="$t('common.rule.input')" />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <template slot="footer">
      <el-button @click="hide">{{ $t("common.button.cancel") }}</el-button>
      <el-button type="primary" :loading="loading" @click="handleConfirm">{{
        $t("common.button.confirm")
      }}</el-button>
    </template>
  </el-dialog>
</template>

<script>
import { editMdmChipStock, getMachineList, checkMdmChipStockUnique, mergeMdmChipStock } from "@/api/lh/mdmChipStock";

export default {
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      form: {},
      rules: {
        factoryCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        chipCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        stockNum: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
      },
    };
  },
  computed: {
    title: function () {
      return this.isEdit
        ? this.$t("common.button.edit")
        : this.$t("common.button.add");
    },
  },
  methods: {
    async save() {
      try {
        this.loading = true;
        const res = await editMdmChipStock(this.form);
        this.$modal.msgSuccess(res.msg);
        this.$emit("success");
        this.hide();
      } catch (error) {
        console.log(error);
      } finally {
        this.loading = false;
      }
    },
    async checkUniqueAndSave() {
      try {
        const res = await checkMdmChipStockUnique(this.form);
        if (res === "0") {
          // 唯一，直接保存
          await this.save();
        } else {
          // 重复，提示是否合并
          this.$confirm(this.$t("mdmChipStock.chipCodeExistsConfirmMerge"), {
            type: "warning",
            confirmButtonText: this.$t("common.button.confirm"),
            cancelButtonText: this.$t("common.button.cancel"),
          }).then(async () => {
            // 确认合并
            try {
              this.loading = true;
              const mergeRes = await mergeMdmChipStock(this.form);
              this.$modal.msgSuccess(mergeRes.msg);
              this.$emit("success");
              this.hide();
            } catch (error) {
              console.log(error);
            } finally {
              this.loading = false;
            }
          }).catch(() => {
            // 取消，不操作
          });
        }
      } catch (error) {
        console.log(error);
      }
    },
    show(data) {
      this.visible = true;
      if (data) {
        this.isEdit = true;
        this.form = { ...data };
      } else {
        this.isEdit = false;
        this.form = {
          factoryCode: "116",
        };
      }
    },
    hide() {
      this.form = {};
      if (this.$refs.form) {
        this.$refs.form.resetFields();
      }
      this.isEdit = false;
      this.visible = false;
    },
    handleConfirm() {
      this.$refs.form.validate((valid) => {
        if (valid) {
          if (this.isEdit) {
            this.save();
          } else {
            this.checkUniqueAndSave();
          }
        }
      });
    },
  },
};
</script>
