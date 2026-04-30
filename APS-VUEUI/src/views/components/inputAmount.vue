<script>
export default {
  props:{
    currency:{
      type:String,
      // required:true,
      default:"USD"
    }
  },
  render(){
    return (
       <el-input-number
        class="w100 amt"
        controls={false}
        min={0}
        max={999999999999999.99}
        props={{...this.$attrs}}
        on={{ ...this.$listeners }}
        precision={this.currency == "VND" ? 0 : 2 }
      />
    )
  },
}
</script>

<style>

</style>
