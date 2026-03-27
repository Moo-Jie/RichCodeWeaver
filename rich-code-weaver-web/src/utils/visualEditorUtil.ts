// 封装被选中或悬停的元素的信息
export interface ElementInfo {
  tagName: string // 元素的标签名（如：DIV、BUTTON等）
  id: string // 元素的ID属性
  className: string // 元素的类名
  textContent: string // 元素的文本内容（截取前100个字符）
  selector: string // 元素的CSS选择器路径
  pagePath: string // 页面路径（包含查询参数和哈希值）
  rect: { // 元素在页面中的位置和尺寸信息
    top: number // 距离顶部的距离
    left: number // 距离左侧的距离
    width: number // 元素宽度
    height: number // 元素高度
  }
}

// 可视化编辑器配置选项接口
export interface VisualEditorOptions {
  onElementSelected?: (elementInfo: ElementInfo) => void // 元素被选中时的回调函数
  onElementHover?: (elementInfo: ElementInfo) => void // 元素被悬停时的回调函数
}

/**
 * 可视化编辑器工具类（含基于 iframe 的可视化工具函数）
 * 提供在iframe中可视化选择和编辑元素的能力
 */
export class visualEditorUtil {
  private iframe: HTMLIFrameElement | null = null // 关联的iframe元素
  private isEditMode = false // 当前是否处于编辑模式
  private options: VisualEditorOptions // 编辑器配置选项

  // 构造函数，初始化配置选项，指定选中、悬停回调函数
  constructor(options: VisualEditorOptions = {}) {
    this.options = options
  }

  /**
   * 初始化编辑器，关联iframe元素
   * @param iframe - 要关联的iframe元素
   */
  init(iframe: HTMLIFrameElement) {
    this.iframe = iframe
  }

  /**
   * 开启编辑模式
   * 注入编辑脚本到iframe中，启用元素选择功能
   */
  enableEditMode() {
    if (!this.iframe) {
      return
    }
    this.isEditMode = true
    // 延迟注入脚本，确保iframe完全加载
    setTimeout(() => {
      this.injectEditScript()
    }, 300)
  }

  /**
   * 关闭编辑模式
   * 通知iframe清除所有编辑效果
   */
  disableEditMode() {
    this.isEditMode = false
    // 通知iframe关闭编辑模式
    this.sendMessageToIframe({
      type: 'TOGGLE_EDIT_MODE',
      editMode: false
    })
    // 清除所有编辑状态
    this.sendMessageToIframe({
      type: 'CLEAR_ALL_EFFECTS'
    })
  }

  /**
   * 切换编辑模式
   * @returns 返回切换后的编辑模式状态
   */
  toggleEditMode() {
    if (this.isEditMode) {
      this.disableEditMode()
    } else {
      this.enableEditMode()
    }
    return this.isEditMode
  }

  /**
   * 强制同步状态并清理
   * 确保非编辑模式下清除所有效果
   */
  syncState() {
    if (!this.isEditMode) {
      this.sendMessageToIframe({
        type: 'CLEAR_ALL_EFFECTS'
      })
    }
  }

  /**
   * 清除选中的元素
   */
  clearSelection() {
    this.sendMessageToIframe({
      type: 'CLEAR_SELECTION'
    })
  }

  /**
   * iframe 加载完成时调用
   * 根据当前编辑模式状态决定是否注入脚本
   */
  onIframeLoad() {
    if (this.isEditMode) {
      // 延迟注入脚本，确保iframe完全加载
      setTimeout(() => {
        this.injectEditScript()
      }, 500)
    } else {
      // 确保非编辑模式时清理状态
      setTimeout(() => {
        this.syncState()
      }, 500)
    }
  }

  /**
   * 处理来自 iframe 的消息
   * @param event - 消息事件对象
   */
  handleIframeMessage(event: MessageEvent) {
    const { type, data } = event.data
    switch (type) {
      case 'ELEMENT_SELECTED': // 处理元素选中消息
        if (this.options.onElementSelected && data.elementInfo) {
          this.options.onElementSelected(data.elementInfo)
        }
        break
      case 'ELEMENT_HOVER': // 处理元素悬停消息
        if (this.options.onElementHover && data.elementInfo) {
          this.options.onElementHover(data.elementInfo)
        }
        break
    }
  }

  /**
   * 向 iframe 发送消息
   * @param message - 要发送的消息对象
   */
  private sendMessageToIframe(message: Record<string, any>) {
    if (this.iframe?.contentWindow) {
      this.iframe.contentWindow.postMessage(message, '*')
    }
  }

  /**
   * 注入编辑脚本到 iframe，用于开启编辑模式
   * 通过轮询等待iframe加载完成后再注入脚本
   */
  private injectEditScript() {
    if (!this.iframe) return

    // 等待iframe加载完成的轮询函数
    const waitForIframeLoad = () => {
      try {
        if (this.iframe!.contentWindow && this.iframe!.contentDocument) {
          // 检查是否已经注入过脚本
          if (this.iframe!.contentDocument.getElementById('visual-edit-script')) {
            // 如果已注入，只需发送启用编辑模式消息
            this.sendMessageToIframe({
              type: 'TOGGLE_EDIT_MODE',
              editMode: true
            })
            return
          }

          // 生成并注入编辑脚本
          const script = this.generateEditScript()
          const scriptElement = this.iframe!.contentDocument.createElement('script')
          scriptElement.id = 'visual-edit-script'
          scriptElement.textContent = script
          this.iframe!.contentDocument.head.appendChild(scriptElement)
        } else {
          // iframe未加载完成，继续轮询
          setTimeout(waitForIframeLoad, 100)
        }
      } catch {
        // 静默处理注入失败（可能是跨域限制）
      }
    }

    waitForIframeLoad()
  }

  /**
   * 生成编辑脚本内容
   * 返回一个自执行函数字符串，包含所有编辑功能逻辑
   */
  private generateEditScript() {
    return `
      (function() {
        let isEditMode = true; // 当前编辑模式状态
        let currentHoverElement = null; // 当前悬停的元素
        let currentSelectedElement = null; // 当前选中的元素

        // 注入编辑模式所需的样式
        function injectStyles() {
          if (document.getElementById('edit-mode-styles')) return;
          const style = document.createElement('style');
          style.id = 'edit-mode-styles';
          style.textContent = \`
            .edit-hover {
              outline: 2px dashed #667eea !important; /* 悬停时的渐变虚线边框 */
              outline-offset: 2px !important;
              cursor: crosshair !important; /* 十字光标 */
              transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
              position: relative !important;
              border-radius: 4px !important;
            }
            .edit-hover::before {
              content: '' !important;
              position: absolute !important;
              top: -6px !important;
              left: -6px !important;
              right: -6px !important;
              bottom: -6px !important;
              background: linear-gradient(135deg, rgba(102, 126, 234, 0.08) 0%, rgba(118, 75, 162, 0.08) 100%) !important;
              pointer-events: none !important;
              z-index: 9998 !important;
              border-radius: 6px !important;
              animation: hoverPulse 2s infinite !important;
            }
            .edit-selected {
              outline: 3px solid #52c41a !important; /* 选中时的绿色实线边框 */
              outline-offset: 2px !important;
              cursor: default !important;
              position: relative !important;
              border-radius: 4px !important;
              box-shadow: 0 0 0 4px rgba(82, 196, 26, 0.15) !important;
              transition: all 0.3s ease !important;
            }
            .edit-selected::before {
              content: '' !important;
              position: absolute !important;
              top: -8px !important;
              left: -8px !important;
              right: -8px !important;
              bottom: -8px !important;
              background: linear-gradient(135deg, rgba(82, 196, 26, 0.1) 0%, rgba(102, 126, 234, 0.1) 100%) !important;
              pointer-events: none !important;
              z-index: 9998 !important;
              border-radius: 8px !important;
              animation: selectedPulse 1.5s infinite !important;
            }
            @keyframes hoverPulse {
              0%, 100% {
                opacity: 0.6;
                transform: scale(1);
              }
              50% {
                opacity: 0.8;
                transform: scale(1.02);
              }
            }
            @keyframes selectedPulse {
              0%, 100% {
                box-shadow: 0 0 0 4px rgba(82, 196, 26, 0.15);
              }
              50% {
                box-shadow: 0 0 0 6px rgba(82, 196, 26, 0.25);
              }
            }
          \`;
          document.head.appendChild(style);
        }

        // 生成元素选择器路径
        function generateSelector(element) {
          const path = [];
          let current = element;
          // 从当前元素向上遍历直到body
          while (current && current !== document.body) {
            let selector = current.tagName.toLowerCase();
            if (current.id) {
              // 优先使用ID选择器
              selector += '#' + current.id;
              path.unshift(selector);
              break;
            }
            if (current.className) {
              // 添加类选择器
              const classes = current.className.split(' ').filter(c => c && !c.startsWith('edit-'));
              if (classes.length > 0) {
                selector += '.' + classes.join('.');
              }
            }
            // 添加子元素位置索引
            const siblings = Array.from(current.parentElement?.children || []);
            const index = siblings.indexOf(current) + 1;
            selector += ':nth-child(' + index + ')';
            path.unshift(selector);
            current = current.parentElement;
          }
          return path.join(' > ');
        }

        // 获取元素的详细信息
        function getElementInfo(element) {
          const rect = element.getBoundingClientRect();
          // 获取HTML文件名后面的部分（查询参数和锚点）
          let pagePath = window.location.search + window.location.hash;
          // 如果没有查询参数和锚点，则显示为空
          if (!pagePath) {
            pagePath = '';
          }

          return {
            tagName: element.tagName,
            id: element.id,
            className: element.className,
            textContent: element.textContent?.trim().substring(0, 100) || '', // 截取前100个字符
            selector: generateSelector(element),
            pagePath: pagePath,
            rect: {
              top: rect.top,
              left: rect.left,
              width: rect.width,
              height: rect.height
            }
          };
        }

        // 清除悬浮效果
        function clearHoverEffect() {
          if (currentHoverElement) {
            currentHoverElement.classList.remove('edit-hover');
            currentHoverElement = null;
          }
        }

        // 清除选中效果
        function clearSelectedEffect() {
          const selected = document.querySelectorAll('.edit-selected');
          selected.forEach(el => el.classList.remove('edit-selected'));
          currentSelectedElement = null;
        }

        let eventListenersAdded = false; // 标记事件监听器是否已添加

        // 添加事件监听器
        function addEventListeners() {
           if (eventListenersAdded) return;

           // 鼠标悬停处理函数
           const mouseoverHandler = (event) => {
             if (!isEditMode) return;

             const target = event.target;
             // 跳过当前已悬停或已选中的元素
             if (target === currentHoverElement || target === currentSelectedElement) return;
             if (target === document.body || target === document.documentElement) return;
             // 跳过脚本和样式元素
             if (target.tagName === 'SCRIPT' || target.tagName === 'STYLE') return;

             clearHoverEffect();
             target.classList.add('edit-hover');
             currentHoverElement = target;

             // 发送悬停元素信息给父窗口
             const elementInfo = getElementInfo(target);
             try {
               window.parent.postMessage({
                 type: 'ELEMENT_HOVER',
                 data: { elementInfo }
               }, '*');
             } catch {
               // 静默处理发送失败
             }
           };

           // 鼠标移出处理函数
           const mouseoutHandler = (event) => {
             if (!isEditMode) return;

             const target = event.target;
             // 只有当鼠标完全离开元素时才清除效果
             if (!event.relatedTarget || !target.contains(event.relatedTarget)) {
               clearHoverEffect();
             }
           };

           // 点击处理函数
           const clickHandler = (event) => {
             if (!isEditMode) return;

             event.preventDefault();
             event.stopPropagation();

             const target = event.target;
             if (target === document.body || target === document.documentElement) return;
             if (target.tagName === 'SCRIPT' || target.tagName === 'STYLE') return;

             clearSelectedEffect();
             clearHoverEffect();

             target.classList.add('edit-selected');
             currentSelectedElement = target;

             // 发送选中元素信息给父窗口
             const elementInfo = getElementInfo(target);
             try {
               window.parent.postMessage({
                 type: 'ELEMENT_SELECTED',
                 data: { elementInfo }
               }, '*');
             } catch {
               // 静默处理发送失败
             }
           };

           // 使用捕获模式确保事件处理
           document.body.addEventListener('mouseover', mouseoverHandler, true);
           document.body.addEventListener('mouseout', mouseoutHandler, true);
           document.body.addEventListener('click', clickHandler, true);
           eventListenersAdded = true;
         }

         function setupEventListeners() {
           addEventListeners();
         }

        // 监听父窗口消息
        window.addEventListener('message', (event) => {
           const { type, editMode } = event.data;
           switch (type) {
             case 'TOGGLE_EDIT_MODE': // 切换编辑模式
               isEditMode = editMode;
               if (isEditMode) {
                 injectStyles();
                 setupEventListeners();
               } else {
                 clearHoverEffect();
                 clearSelectedEffect();
               }
               break;
             case 'CLEAR_SELECTION': // 清除选中
               clearSelectedEffect();
               break;
             case 'CLEAR_ALL_EFFECTS': // 清除所有效果
               isEditMode = false;
               clearHoverEffect();
               clearSelectedEffect();
               const tip = document.getElementById('edit-tip');
               if (tip) tip.remove(); // 移除编辑提示
               break;
           }
         });

         // 初始化编辑模式
         injectStyles();
         setupEventListeners();
      })();
    `
  }
}
