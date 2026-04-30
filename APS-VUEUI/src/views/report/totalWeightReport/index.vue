
<template>
  <basic-container  v-loading="loading">

    <iframe
    style="width: 100%; height: 90%;"
    :src="url" frameborder="0"></iframe>


  </basic-container>
</template>
<script>

import { totalWeightReport } from "@/api/monthplan/report";
export default {
  name: "totalWeightReport",
  components: {
    // tltUpload,
  },
  dicts: [],
  data() {
    return {
      url:'',
      loading:false
    };
  },
  computed: {

  },
  methods: {
   async getUrl() {
    this.loading=true
    try{
      let res=await totalWeightReport({})
      this.url=res
    }catch(err){

    }finally{
      this.loading=false
    }

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