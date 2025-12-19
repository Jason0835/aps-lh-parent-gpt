<script>
import VueSeamlessScroll from 'vue-seamless-scroll'
export default {
  components: { VueSeamlessScroll },
  props: {
    data: {type:Array, default:() => []},
    columns: {type:Array, default:() => []},
    height: {
      type: String | Number,
    },
    scrollSpeed: {
        type:Number,
        default: 0.3
    },
    limitMoveNum: {
        type:Number,
        default: 10
    }
  },
  data() {
    return {
    };
  },

  created() {

  },
  computed:{
    classOptionUp() {
      return {
        step: this.scrollSpeed, // 数值越大速度滚动越快
        limitMoveNum: this.limitMoveNum, // 开始无缝滚动的数据量 this.dataList.length
        hoverStop: true, // 是否开启鼠标悬停stop
        direction: 1, // 0向下 1向上 2向左 3向右
        openWatch: true, // 开启数据实时监控刷新dom
        singleHeight: 0, // 单步运动停止的高度(默认值0是无缝不停止的滚动) direction => 0/1
        singleWidth: 0, // 单步运动停止的宽度(默认值0是无缝不停止的滚动) direction => 2/3
        waitTime: 1000 // 单步运动停止的时间(默认值1000ms)
      }
    }
  },
  methods: {
  },
  render() {
    return (
      <div class="warpper" style={{height: this.height}}>
        <table class="scroll-table">
            <thead>
                <tr>
                    {this.columns.map((col)=>{
                        return (
                            <th width={col.width}>{col.label}</th>
                        )
                    })}
                </tr>
            </thead>
        </table>
        <div class="table-warpper" style={{height: `calc(${this.height} - 32px)`}}>
            <VueSeamlessScroll data={this.data} class="seamless-warp" class-option={this.classOptionUp}>
                <table ref="scrollTableRef" class="scroll-table"
                    style={{ '--speed': this.scrollSpeed }}>
                    <tbody>
                        {this.data.map((row,index) => {
                            return (<tr style="{{background: index % 2 == 0 'linear-gradient( 270deg, #2AACF533 0%, #00A3FF33 100%)' : ''}}">
                                {this.columns.map((col)=>{
                                    return (
                                        <td width={col.width}>{col.render ? col.render(row) : row[col.prop]}</td>
                                    )
                                })}
                            </tr>)
                        })}
                    </tbody>
                </table>
            </VueSeamlessScroll>
        </div>
    </div>
    );
  },
};
</script>
<style  scoped>


.scroll-table{
  width: 100%;
  text-align: center;
}

.scroll-table tr {
  height: 32px;
  font-weight: 400;
  font-size: 14px;
  color: #FFFFFF;
  line-height: 32px;
}

.scroll-table tr td {
    color: #FFFFFF;
  vertical-align: middle;
  line-height: normal;
}

.scroll-table thead {
  background: linear-gradient( 270deg, #2AACF577 0%, #00A3FF77 100%);
}

.scroll-table tbody tr:nth-of-type(even) {
  background: linear-gradient( 270deg, #2AACF533 0%, #00A3FF33 100%);
}

.table-warpper{
  overflow: hidden;
}

.table-warpper.scroll .scroll-table{
  animation: calc(12s + (80s * var(--speed))) scroll-table linear infinite normal;
  transition-delay: 1s;
  animation-delay: 1s;
}


@keyframes scroll-table {
  0% {
    transform: translateY(0px);
    -webkit-transform: translateX(0px);
  }
  50% {
    transform: translateY(calc(-100% + 224px));
    -webkit-transform: translateY(calc(-100% + 224px));
  }
  100% {
    transform: translateY(0px);
    -webkit-transform: translateY(0px);
  }
}
</style>