
<template>
  <basic-container>
    <page-table
      tableRef="ProductConstructionMainTable"
      :calcHeight="true"
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
    >
      <template slot="header">
        <el-button
          type="primary"
          v-hasPermi="['cx:productConstruction:add']"
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}</el-button
        >
        <el-button
          type="warning"
          v-hasPermi="['cx:machine:edit']"
          :disabled="selection.length !== 1"
          @click="handleEdit(selection[0])"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        >
        <el-button
          v-hasPermi="['cx:productConstruction:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >
        <el-button
          @click="handleExport"
          v-hasPermi="['cx:productConstruction:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
      </template>
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <tlt-upload
      ref="tltUpload"
      downloadUrl="/cx/productConstruction/importTemplate"
      uploadUrl="/cx/productConstruction/importData"
      @uploadSuccess="getList"
    />
    <infoDialog ref="infoRef" @success="getList" />
    <addDialog ref="addRef" @success="getList" />
    <updateStageDialog ref="stageRef" @success="getList" />
    <versionDialog ref="versionRef" @success="getList" />
  </basic-container>
</template>
<script>
//lib
// import moment from "moment";
//utils
import { downloadLink } from "@/utils/request";
//interface
import {
  listProductConstruction,
  removeProductConstruction,
} from "@/api/cx/productConstruction.js";
//components
import tltUpload from "@/components/tltUpload/tltUpload.vue";

import infoDialog from "./components/infoDialog.vue";
import addDialog from "./components/addDialog.vue";
import updateStageDialog from "./components/updateStageDialog.vue";
import versionDialog from "./components/versionDialog.vue";

export default {
  name: "ProductConstruction",
  components: {
    tltUpload,
    infoDialog,
    addDialog,
    updateStageDialog,
    versionDialog,
  },
  dicts: ["PRODUCTION_STAGE", "HALF_PARTS_CODE"],
  provide() {
    return {
      parentDict: this.dict,
    };
  },
  data() {
    return {
      loading: false,
      data: [],
      selection: [],
      page: {
        current: 1,
        pageSize: 20,
        total: 0,
      },
      sort: {},
      search: {
        mainPlanMonth: "",
      },
      query: {
        mainPlanMonth: "",
      },
      importDefaultValue: {},
      importRules: {},
    };
  },
  computed: {
    columns() {
      let columns = [
        { type: "selection", fixed: "left" },
        {
          label: this.$t("ui.construction.baseInfo"),
          align: "center",
          children: [
            // {
            //   prop: "id",
            //   align: "center",
            //   halign: "center",
            //   label: "id",
            // },
            {
              prop: "sapCode",
              align: "center",
              halign: "center",
              label: this.$t("ui.data.column.productConstruction.sapCode"),
            },
            {
              prop: "embryoCode",
              align: "center",
              halign: "center",
              label: this.$t("ui.data.column.productConstruction.embryoCode"),
            },
            {
              prop: "embryoVersion",
              align: "center",
              halign: "center",
              label: this.$t(
                "ui.data.column.productConstruction.embryoVersion"
              ),
            },
            {
              prop: "productionStage",
              align: "center",
              halign: "center",
              label: this.$t(
                "ui.data.column.productConstruction.productionStage"
              ),
              render: ({ row }) => {
                let value = this.selectDictLabel(
                  this.dict.type.PRODUCTION_STAGE,
                  row.productionStage
                );

                return (
                  <text-button
                    onClick={() => {
                      this.handleChangeProductionStage(row);
                    }}
                  >
                    {value}
                  </text-button>
                );
              },
            },
            {
              prop: "consType",
              align: "center",
              halign: "center",
              label: this.$t("ui.data.column.productConstruction.consType"),
            },
            {
              prop: "dimension",
              align: "center",
              halign: "center",
              label: this.$t("ui.data.column.productConstruction.dimension"),
            },
            {
              prop: "specDesc",
              align: "left",
              halign: "center",
              label: this.$t("ui.data.column.productConstruction.specDesc"),
              width: 150,
            },
            {
              prop: "noseWidth",
              align: "right",
              halign: "center",
              label: this.$t("ui.data.column.productConstruction.noseWidth"),
            },
            {
              prop: "flipDiscDiameter",
              align: "right",
              halign: "center",
              label: this.$t(
                "ui.data.column.productConstruction.flipDiscDiameter"
              ),
            },
            {
              prop: "sectionWidth",
              align: "right",
              halign: "center",
              label: this.$t("ui.data.column.productConstruction.sectionWidth"),
            },
            {
              prop: "fitDrumPerimeter",
              align: "right",
              halign: "center",
              label: this.$t(
                "ui.data.column.productConstruction.fitDrumPerimeter"
              ),
            },
            {
              prop: "chuckDiameter",
              align: "right",
              halign: "center",
              label: this.$t(
                "ui.data.column.productConstruction.chuckDiameter"
              ),
            },
            {
              prop: "stretchWidth",
              align: "right",
              halign: "center",
              label: this.$t("ui.data.column.productConstruction.stretchWidth"),
            },
            {
              prop: "qualitativeWidth",
              align: "right",
              halign: "center",
              label: this.$t(
                "ui.data.column.productConstruction.qualitativeWidth"
              ),
            },
            {
              prop: "embryoCircle",
              align: "right",
              halign: "center",
              label: this.$t("ui.data.column.productConstruction.embryoCircle"),
            },
          ],
        },
        {
          label: this.$t("ui.construction.carcassCloth"),
          align: "center",
          children: [
            {
              prop: "tireFabricCode1",
              align: "center",
              halign: "center",
              label: this.$t(
                "ui.data.column.productConstruction.tireFabricCode1"
              ),
            },
            {
              prop: "tireFabricSap1",
              align: "center",
              halign: "center",
              label: this.$t(
                "ui.data.column.productConstruction.tireFabricSap1"
              ),
            },
            {
              prop: "tireFabric1Version",
              align: "center",
              halign: "center",
              label: this.$t(
                "ui.data.column.productConstruction.tireFabric1Version"
              ),
              render: ({ row }) => {
                let value = row.tireFabric1Version;

                if (this.isEmpty(value)) {
                  value = this.$t(
                    "ui.data.column.productConstruction.noVersion"
                  );
                }

                return (
                  <text-button
                    onClick={() =>
                      this.handleVersionChange(row, "TIRE_FABRIC1")
                    }
                  >
                    {value}
                  </text-button>
                );
              },
            },
            {
              prop: "tireFabricCraft1",
              align: "right",
              halign: "center",
              label: this.$t(
                "ui.data.column.productConstruction.tireFabricCraft1"
              ),
            },
            {
              prop: "tireFabricSideRubber",
              align: "center",
              halign: "center",
              label: this.$t(
                "ui.data.column.productConstruction.tireFabricSideRubber"
              ),
            },
            {
              prop: "tireFabricCode2",
              align: "center",
              halign: "center",
              label: this.$t(
                "ui.data.column.productConstruction.tireFabricCode2"
              ),
            },
            {
              prop: "tireFabricSap2",
              align: "center",
              halign: "center",
              label: this.$t(
                "ui.data.column.productConstruction.tireFabricSap2"
              ),
            },
            {
              prop: "tireFabric2Version",
              align: "center",
              halign: "center",
              label: this.$t(
                "ui.data.column.productConstruction.tireFabric2Version"
              ),
              render: ({ row }) => {
                let value = row.tireFabric2Version;

                if (this.isEmpty(value)) {
                  value = this.$t(
                    "ui.data.column.productConstruction.noVersion"
                  );
                }

                return (
                  <text-button
                    onClick={() =>
                      this.handleVersionChange(row, "TIRE_FABRIC2")
                    }
                  >
                    {value}
                  </text-button>
                );
              },
            },
            {
              prop: "tireFabricCraft2",
              align: "right",
              halign: "center",
              label: this.$t(
                "ui.data.column.productConstruction.tireFabricCraft2"
              ),
            },
            {
              prop: "tireFabricCode3",
              align: "center",
              halign: "center",
              label: this.$t(
                "ui.data.column.productConstruction.tireFabricCode3"
              ),
            },
            {
              prop: "tireFabricSap3",
              align: "center",
              halign: "center",
              label: this.$t(
                "ui.data.column.productConstruction.tireFabricSap3"
              ),
            },
            {
              prop: "tireFabric3Version",
              align: "center",
              halign: "center",
              label: this.$t(
                "ui.data.column.productConstruction.tireFabric3Version"
              ),
              render: ({ row }) => {
                let value = row.tireFabric3Version;

                if (this.isEmpty(value)) {
                  value = this.$t(
                    "ui.data.column.productConstruction.noVersion"
                  );
                }

                return (
                  <text-button
                    onClick={() =>
                      this.handleVersionChange(row, "TIRE_FABRIC3")
                    }
                  >
                    {value}
                  </text-button>
                );
              },
            },
            {
              prop: "tireFabricCraft3",
              align: "right",
              halign: "center",
              label: this.$t(
                "ui.data.column.productConstruction.tireFabricCraft3"
              ),
            },
            {
              prop: "originalLineCode",
              align: "center",
              halign: "center",
              label: this.$t(
                "ui.data.column.productConstruction.originalLineCode"
              ),
            },
            {
              prop: "cordSpec",
              align: "center",
              halign: "center",
              label: this.$t("ui.data.column.productConstruction.cordSpec"),
            },
            {
              prop: "cordSap",
              align: "center",
              halign: "center",
              label: this.$t("ui.data.column.productConstruction.cordSap"),
            },
            {
              prop: "cordVersion",
              align: "center",
              halign: "center",
              label: this.$t("ui.data.column.productConstruction.cordVersion"),
              render: ({ row }) => {
                let value = row.cordVersion;

                if (this.isEmpty(value)) {
                  value = this.$t(
                    "ui.data.column.productConstruction.noVersion"
                  );
                }

                return (
                  <text-button
                    onClick={() => this.handleVersionChange(row, "CORD")}
                  >
                    {value}
                  </text-button>
                );
              },
            },
            {
              prop: "reinforceSealGlue",
              align: "center",
              halign: "center",
              label: this.$t(
                "ui.data.column.productConstruction.reinforceSealGlue"
              ),
            },
          ],
        },
        {
          label: this.$t("ui.construction.lining"),
          align: "center",
          children: [
            {
              prop: "insideRubber",
              align: "center",
              halign: "center",
              label: this.$t("ui.data.column.productConstruction.insideRubber"),
            },
            {
              prop: "insideCode",
              align: "center",
              halign: "center",
              label: this.$t("ui.data.column.productConstruction.insideCode"),
            },
            {
              prop: "insideSap",
              align: "center",
              halign: "center",
              label: this.$t("ui.data.column.productConstruction.insideSap"),
            },
            {
              prop: "insideVersion",
              align: "center",
              halign: "center",
              label: this.$t(
                "ui.data.column.productConstruction.insideVersion"
              ),
              render: ({ row }) => {
                let value = row.insideVersion;

                if (this.isEmpty(value)) {
                  value = this.$t(
                    "ui.data.column.productConstruction.noVersion"
                  );
                }

                return (
                  <text-button
                    onClick={() => this.handleVersionChange(row, "INSIDE")}
                  >
                    {value}
                  </text-button>
                );
              },
            },
            {
              prop: "insideCraft",
              align: "right",
              halign: "center",
              label: this.$t("ui.data.column.productConstruction.insideCraft"),
            },
          ],
        },
        {
          label: this.$t("ui.construction.sidewall"),
          align: "center",
          children: [
            {
              prop: "sidewallCode",
              align: "center",
              halign: "center",
              label: this.$t("ui.data.column.productConstruction.sidewallCode"),
            },
            {
              prop: "sidewallSap",
              align: "center",
              halign: "center",
              label: this.$t("ui.data.column.productConstruction.sidewallSap"),
            },
            {
              prop: "sidewallVersion",
              align: "center",
              halign: "center",
              label: this.$t(
                "ui.data.column.productConstruction.sidewallVersion"
              ),
              render: ({ row }) => {
                let value = row.sidewallVersion;

                if (this.isEmpty(value)) {
                  value = this.$t(
                    "ui.data.column.productConstruction.noVersion"
                  );
                }

                return (
                  <text-button
                    onClick={() => this.handleVersionChange(row, "SIDEWALL")}
                  >
                    {value}
                  </text-button>
                );
              },
            },
            {
              prop: "sidewallCraft",
              align: "right",
              halign: "center",
              label: this.$t(
                "ui.data.column.productConstruction.sidewallCraft"
              ),
            },
            {
              prop: "sidewallMouthPlate",
              align: "center",
              halign: "center",
              label: this.$t(
                "ui.data.column.productConstruction.sidewallMouthPlate"
              ),
            },
            {
              prop: "sidewallCenter",
              align: "center",
              halign: "center",
              label: this.$t(
                "ui.data.column.productConstruction.sidewallCenter"
              ),
            },
            {
              prop: "sidewallLength",
              align: "right",
              halign: "center",
              label: this.$t(
                "ui.data.column.productConstruction.sidewallLength"
              ),
            },
            {
              prop: "sidewallRubber",
              align: "center",
              halign: "center",
              label: this.$t(
                "ui.data.column.productConstruction.sidewallRubber"
              ),
            },
            {
              prop: "sidewallWeight",
              align: "right",
              halign: "center",
              label: this.$t(
                "ui.data.column.productConstruction.sidewallWeight"
              ),
            },
            {
              prop: "sidewallWearpRubberWeight",
              align: "right",
              halign: "center",
              label: this.$t(
                "ui.data.column.productConstruction.sidewallWearpRubberWeight"
              ),
            },
          ],
        },
        {
          label: this.$t("ui.construction.supportingGlue"),
          align: "center",
          children: [
            {
              prop: "supportCode",
              align: "center",
              halign: "center",
              label: this.$t("ui.data.column.productConstruction.supportCode"),
            },
            {
              prop: "supportRubberCode",
              align: "center",
              halign: "center",
              label: this.$t(
                "ui.data.column.productConstruction.supportRubberCode"
              ),
            },
            {
              prop: "supportLength",
              align: "right",
              halign: "center",
              label: this.$t(
                "ui.data.column.productConstruction.supportLength"
              ),
            },
          ],
        },
        {
          label: this.$t("ui.construction.steelRing"),
          align: "center",
          children: [
            {
              prop: "beadCode",
              align: "center",
              halign: "center",
              label: this.$t("ui.data.column.productConstruction.beadCode"),
            },
            {
              prop: "beadSap",
              align: "center",
              halign: "center",
              label: this.$t("ui.data.column.productConstruction.beadSap"),
            },
            {
              prop: "beadVersion",
              align: "center",
              halign: "center",
              label: this.$t("ui.data.column.productConstruction.beadVersion"),
              render: ({ row }) => {
                let value = row.beadVersion;

                if (this.isEmpty(value)) {
                  value = this.$t(
                    "ui.data.column.productConstruction.noVersion"
                  );
                }

                return (
                  <text-button
                    onClick={() => this.handleVersionChange(row, "BEAD")}
                  >
                    {value}
                  </text-button>
                );
              },
            },
            {
              prop: "beadArrange",
              align: "left",
              halign: "center",
              label: this.$t("ui.data.column.productConstruction.beadArrange"),
            },
            {
              prop: "beadType",
              align: "center",
              halign: "center",
              label: this.$t("ui.data.column.productConstruction.beadType"),
            },
          ],
        },
        {
          label: this.$t("ui.construction.hexagonRing"),
          align: "center",
          children: [
            {
              prop: "tireRingCode",
              align: "center",
              halign: "center",
              label: this.$t("ui.data.column.productConstruction.tireRingCode"),
            },
            {
              prop: "tireRingSap",
              align: "center",
              halign: "center",
              label: this.$t("ui.data.column.productConstruction.tireRingSap"),
            },
            {
              prop: "tireRingVersion",
              align: "center",
              halign: "center",
              label: this.$t(
                "ui.data.column.productConstruction.tireRingVersion"
              ),
              render: ({ row }) => {
                let value = row.tireRingVersion;

                if (this.isEmpty(value)) {
                  value = this.$t(
                    "ui.data.column.productConstruction.noVersion"
                  );
                }

                return (
                  <text-button
                    onClick={() => this.handleVersionChange(row, "TIRE_RING")}
                  >
                    {value}
                  </text-button>
                );
              },
            },
            {
              prop: "apexCode",
              align: "center",
              halign: "center",
              label: this.$t("ui.data.column.productConstruction.apexCode"),
            },
            {
              prop: "hexagonRubberCode",
              align: "center",
              halign: "center",
              label: this.$t(
                "ui.data.column.productConstruction.hexagonRubberCode"
              ),
            },
            {
              prop: "hexagonMouthPlate",
              align: "center",
              halign: "center",
              label: this.$t(
                "ui.data.column.productConstruction.hexagonMouthPlate"
              ),
            },
            {
              prop: "hexagonRubberDimension",
              align: "left",
              halign: "center",
              label: this.$t(
                "ui.data.column.productConstruction.hexagonRubberDimension"
              ),
            },
            {
              prop: "apexWeight",
              align: "right",
              halign: "center",
              label: this.$t("ui.data.column.productConstruction.apexWeight"),
            },
          ],
        },
        {
          label: this.$t("ui.construction.steelStrip"),
          align: "center",
          children: [
            {
              prop: "beltCode1",
              align: "center",
              halign: "center",
              label: this.$t("ui.data.column.productConstruction.beltCode1"),
            },
            {
              prop: "beltSap1",
              align: "center",
              halign: "center",
              label: this.$t("ui.data.column.productConstruction.beltSap1"),
            },
            {
              prop: "belt1Version",
              align: "center",
              halign: "center",
              label: this.$t("ui.data.column.productConstruction.belt1Version"),
              render: ({ row }) => {
                let value = row.belt1Version;

                if (this.isEmpty(value)) {
                  value = this.$t(
                    "ui.data.column.productConstruction.noVersion"
                  );
                }

                return (
                  <text-button
                    onClick={() => this.handleVersionChange(row, "BELT1")}
                  >
                    {value}
                  </text-button>
                );
              },
            },
            {
              prop: "beltCraft1",
              align: "right",
              halign: "center",
              label: this.$t("ui.data.column.productConstruction.beltCraft1"),
            },
            {
              prop: "beltSideRubber1",
              align: "center",
              halign: "center",
              label: this.$t(
                "ui.data.column.productConstruction.beltSideRubber1"
              ),
            },
            {
              prop: "beltRubber1",
              align: "center",
              halign: "center",
              label: this.$t("ui.data.column.productConstruction.beltRubber1"),
            },
            {
              prop: "beltCode2",
              align: "center",
              halign: "center",
              label: this.$t("ui.data.column.productConstruction.beltCode2"),
            },
            {
              prop: "beltSap2",
              align: "center",
              halign: "center",
              label: this.$t("ui.data.column.productConstruction.beltSap2"),
            },
            {
              prop: "belt2Version",
              align: "center",
              halign: "center",
              label: this.$t("ui.data.column.productConstruction.belt2Version"),
              render: ({ row }) => {
                let value = row.belt2Version;

                if (this.isEmpty(value)) {
                  value = this.$t(
                    "ui.data.column.productConstruction.noVersion"
                  );
                }

                return (
                  <text-button
                    onClick={() => this.handleVersionChange(row, "BELT2")}
                  >
                    {value}
                  </text-button>
                );
              },
            },
            {
              prop: "beltCraft2",
              align: "right",
              halign: "center",
              label: this.$t("ui.data.column.productConstruction.beltCraft2"),
            },
            {
              prop: "beltSideRubber2",
              align: "center",
              halign: "center",
              label: this.$t(
                "ui.data.column.productConstruction.beltSideRubber2"
              ),
            },
            {
              prop: "beltRubber2",
              align: "center",
              halign: "center",
              label: this.$t("ui.data.column.productConstruction.beltRubber2"),
            },
            {
              prop: "beltCuttingAngle",
              align: "right",
              halign: "center",
              label: this.$t(
                "ui.data.column.productConstruction.beltCuttingAngle"
              ),
            },

            {
              prop: "articleCrownSpec",
              align: "center",
              halign: "center",
              label: this.$t(
                "ui.data.column.productConstruction.articleCrownSpec"
              ),
            },
            {
              prop: "articleCrownSap",
              align: "center",
              halign: "center",
              label: this.$t(
                "ui.data.column.productConstruction.articleCrownSap"
              ),
            },
            {
              prop: "articleCrownVersion",
              align: "center",
              halign: "center",
              label: this.$t(
                "ui.data.column.productConstruction.articleCrownVersion"
              ),
              render: ({ row }) => {
                let value = row.articleCrownVersion;

                if (this.isEmpty(value)) {
                  value = this.$t(
                    "ui.data.column.productConstruction.noVersion"
                  );
                }

                return (
                  <text-button
                    onClick={() =>
                      this.handleVersionChange(row, "ARTICLE_CROWN")
                    }
                  >
                    {value}
                  </text-button>
                );
              },
            },
          ],
        },
        {
          label: this.$t("ui.construction.crownBand"),
          align: "center",
          children: [
            {
              prop: "articleCrownCode",
              align: "center",
              halign: "center",
              label: this.$t(
                "ui.data.column.productConstruction.articleCrownCode"
              ),
            },
          ],
        },
        {
          label: this.$t("ui.construction.tread"),
          align: "center",
          children: [
            {
              prop: "treadCode",
              align: "center",
              halign: "center",
              label: this.$t("ui.data.column.productConstruction.treadCode"),
              width: 150,
            },
            {
              prop: "treadSap",
              align: "center",
              halign: "center",
              label: this.$t("ui.data.column.productConstruction.treadSap"),
            },
            {
              prop: "treadVersion",
              align: "center",
              halign: "center",
              label: this.$t("ui.data.column.productConstruction.treadVersion"),
              render: ({ row }) => {
                let value = row.treadVersion;

                if (this.isEmpty(value)) {
                  value = this.$t(
                    "ui.data.column.productConstruction.noVersion"
                  );
                }

                return (
                  <text-button
                    onClick={() => this.handleVersionChange(row, "TREAD")}
                  >
                    {value}
                  </text-button>
                );
              },
            },
            {
              prop: "treadShoulderWidth",
              align: "right",
              halign: "center",
              label: this.$t(
                "ui.data.column.productConstruction.treadShoulderWidth"
              ),
            },
            {
              prop: "treadShoulderJwidth",
              align: "right",
              halign: "center",
              label: this.$t(
                "ui.data.column.productConstruction.treadShoulderJwidth"
              ),
            },
            {
              prop: "treadShoulderLength",
              align: "right",
              halign: "center",
              label: this.$t(
                "ui.data.column.productConstruction.treadShoulderLength"
              ),
            },
            {
              prop: "treadRubberCategory",
              align: "left",
              halign: "center",
              label: this.$t(
                "ui.data.column.productConstruction.treadRubberCategory"
              ),
            },
            {
              prop: "tireCrownUpWidthWeight",
              align: "right",
              halign: "center",
              label: this.$t(
                "ui.data.column.productConstruction.tireCrownUpWidthWeight"
              ),
            },
            {
              prop: "tireCrownDownWidthWeight",
              align: "right",
              halign: "center",
              label: this.$t(
                "ui.data.column.productConstruction.tireCrownDownWidthWeight"
              ),
            },
            {
              prop: "tireWingWidthWeight",
              align: "right",
              halign: "center",
              label: this.$t(
                "ui.data.column.productConstruction.tireWingWidthWeight"
              ),
            },
            {
              prop: "primerWeight",
              align: "right",
              halign: "center",
              label: this.$t("ui.data.column.productConstruction.primerWeight"),
            },
            {
              prop: "conductingResinWeight",
              align: "right",
              halign: "center",
              label: this.$t(
                "ui.data.column.productConstruction.conductingResinWeight"
              ),
            },
            {
              prop: "treadMouthPlate",
              align: "center",
              halign: "center",
              label: this.$t(
                "ui.data.column.productConstruction.treadMouthPlate"
              ),
            },
          ],
        },
      ];

      return columns;
    },
    searchColumns() {
      return [
        {
          label: this.$t("ui.data.column.sapSpecMoldUse.embryoCode"),
          prop: "colorCode",
        },
        {
          label: this.$t("ui.data.column.productConstruction.embryoVersion"),
          prop: "embryoVersion",
        },
        {
          label: this.$t("ui.data.column.productConstruction.productionStage"),
          prop: "productionStage",
          type: "select", //PRODUCTION_STAGE
          dictData: this.dict.type.PRODUCTION_STAGE,
        },
        {
          label: this.$t(
            "ui.data.column.productConstruction.halfPartsQueryType"
          ),
          prop: "halfPartsQueryType",
          type: "select", //HALF_PARTS_CODE
          dictData: this.dict.type.HALF_PARTS_CODE,
        },
        {
          label: this.$t(
            "ui.data.column.productConstruction.halfPartsQueryCode"
          ),
          prop: "halfPartsQueryCode",
        },
      ];
    },
  },
  methods: {
    handleAdd() {
      if (this.$refs.addRef) {
        this.$refs.addRef.show();
      }
    },
    handleEdit() {
      if (this.$refs.infoRef) {
        this.$refs.infoRef.show(this.selection[0]);
      }
    },
    handleDelete(row) {
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        const ids = row.id;
        this.loading = true;
        removeProductConstruction({ ids })
          .then((data) => {
            this.$modal.msgSuccess(data.msg);
            this.$set(this.page, "current", 1);
            this.getList();
          })
          .catch((error) => {
            console.log(error);
            this.loading = false;
          });
      });
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
    handelSuccess() {
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
    handleExport() {
      downloadLink("/cx/productConstruction/export", this.formatParams(false));
    },
    handleChangeProductionStage(row) {
      if (this.$refs.stageRef) {
        this.$refs.stageRef.show({
          id: row.id,
          embryoCode: row.embryoCode,
          embryoVersion: row.embryoVersion,
          productionStage: row.productionStage,
        });
      }
    },
    handleVersionChange(row, type) {
      if (this.$refs.versionRef) {
        this.$refs.versionRef.show(row, type);
      }
    },

    // utils
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
        params.createTimeStart = params.createTime[0];
        params.createTimeEnd = params.createTime[1];
        params.createTime = undefined;
      }

      return params;
    },
    // api
    async getList() {
      try {
        this.loading = true;
        const data = await listProductConstruction(this.formatParams());
        this.data = data.rows;
        this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
  },
  created() {},
  activated() {
    this.getList();
  },
};
</script>
<style lang="scss" scoped>
.more-btn {
  margin: 2px 0;
  width: 100%;
}
</style>
