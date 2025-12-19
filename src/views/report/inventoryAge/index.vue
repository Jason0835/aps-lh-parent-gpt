
<template>
  <basic-container>

    <iframe
    style="width: 100%; height: 90%;"
    :src="url" frameborder="0"></iframe>


  </basic-container>
</template>
<script>

import { inventoryAgeAnalysisReport } from "@/api/monthplan/report";
export default {
  name: "inventoryAge",
  components: {
    // tltUpload,
  },
  dicts: ["biz_factory_name", "unit", "biz_brand_type"],
  data() {
    return {
      url:''
    };
  },
  computed: {

  },
  methods: {
   async getUrl() {
     let res=await inventoryAgeAnalysisReport()
     this.url=res
    },
  },
  created() {
    this.getUrl()

  },
  activated() {

  },
};
</script>
<style lang="scss" scoped>
.more-btn {
  margin: 2px 0;
  width: 100%;
}
</style>
<style lang="scss" scoped>
.stat-info {
  font-size: 12px;
  color: #5f5858;
  span {
    margin-left: 5px;
  }
}
</style>