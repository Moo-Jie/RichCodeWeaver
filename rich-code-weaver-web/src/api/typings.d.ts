declare namespace API {
  type AppAddRequest = {
    initPrompt?: string
    generatorType?: 'AI_STRATEGY' | 'HTML' | 'MULTI_FILE' | 'VUE_PROJECT'
    genMode?: 'workflow' | 'agent'
  }

  type AppAdminUpdateRequest = {
    id?: number
    appName?: string
    cover?: string
    priority?: number
  }

  type AppCodeGenRequest = {
    appId?: number
    message?: string
  }

  type AppDeployRequest = {
    appId?: number
  }

  type AppQueryRequest = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    id?: number
    appName?: string
    cover?: string
    initPrompt?: string
    codeGenType?: string
    deployKey?: string
    priority?: number
    userId?: number
  }

  type AppUpdateRequest = {
    id?: number
    appName?: string
  }

  type AppVO = {
    id?: number
    appName?: string
    cover?: string
    initPrompt?: string
    codeGenType?: string
    genMode?: 'workflow' | 'agent'
    deployKey?: string
    deployedTime?: string
    priority?: number
    userId?: number
    createTime?: string
    updateTime?: string
    ownershipType?: 'mine' | 'collaborator'
    user?: UserVO
  }

  type BaseResponseAppVO = {
    code?: number
    data?: AppVO
    message?: string
  }

  type BaseResponseBoolean = {
    code?: number
    data?: boolean
    message?: string
  }

  type BaseResponseListAppVO = {
    code?: number
    data?: AppVO[]
    message?: string
  }

  type BaseResponseLoginUserVO = {
    code?: number
    data?: LoginUserVO
    message?: string
  }

  type BaseResponseLong = {
    code?: number
    data?: number
    message?: string
  }

  type BaseResponsePageAppVO = {
    code?: number
    data?: PageAppVO
    message?: string
  }

  type BaseResponsePageChatHistory = {
    code?: number
    data?: PageChatHistory
    message?: string
  }

  type BaseResponsePageUserVO = {
    code?: number
    data?: PageUserVO
    message?: string
  }

  type BaseResponseString = {
    code?: number
    data?: string
    message?: string
  }

  type BaseResponseUser = {
    code?: number
    data?: User
    message?: string
  }

  type BaseResponseUserVO = {
    code?: number
    data?: UserVO
    message?: string
  }

  type ChatHistory = {
    id?: number
    message?: string
    messageType?: string
    appId?: number
    userId?: number
    createTime?: string
    updateTime?: string
    isDelete?: number
  }

  type ChatHistoryQueryRequest = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    id?: number
    lastCreateTime?: string
    message?: string
    messageType?: string
    appId?: number
    userId?: number
  }

  type chatToGenCodeParams = {
    appCodeGenRequest: AppCodeGenRequest
  }

  type chatToGenCodeStreamParams = {
    appId: number
    message: string
    isWorkflow?: boolean
  }

  type deleteByIdParams = {
    id: number
  }

  type DeleteRequest = {
    id?: number
  }

  type downloadCodeZipFileParams = {
    appId: number
  }

  type getAppVOByIdByAdminParams = {
    id: number
  }

  type getAppVOByIdParams = {
    id: number
  }

  type getUserByIdParams = {
    id: number
  }

  type getUserVOByIdParams = {
    id: number
  }

  type listAppChatHistoryByPageParams = {
    appId: number
    pageSize?: number
    lastCreateTime?: string
  }

  type LoginUserVO = {
    id?: number
    userAccount?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    userIdentity?: string
    userIndustry?: string
    phone?: string
    email?: string
    createTime?: string
    updateTime?: string
  }

  type PageAppVO = {
    records?: AppVO[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type PageChatHistory = {
    records?: ChatHistory[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type PageUserVO = {
    records?: UserVO[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type resetUserPasswordParams = {
    userId: number
  }

  type ServerSentEventString = true

  type uploadAppCoverParams = {
    appId: number
  }

  type User = {
    id?: number
    userAccount?: string
    userPassword?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    userIdentity?: string
    userIndustry?: string
    editTime?: string
    createTime?: string
    updateTime?: string
    isDelete?: number
  }

  type UserAddRequest = {
    userName?: string
    userAccount?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
  }

  type UserLoginRequest = {
    email?: string
    userPassword?: string
  }

  type UserQueryRequest = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    id?: number
    userName?: string
    userAccount?: string
    userProfile?: string
    userRole?: string
  }

  type UserRegisterRequest = {
    email?: string
    userName?: string
    emailCode?: string
    userPassword?: string
    checkPassword?: string
  }

  type SendEmailCodeRequest = {
    email?: string
    captchaId?: string
    captchaAnswer?: string
  }

  type CaptchaResponse = {
    captchaId?: string
    captchaImage?: string
  }

  type BaseResponseCaptcha = {
    code?: number
    data?: CaptchaResponse
    message?: string
  }

  type UserUpdatePasswordRequest = {
    userId?: number
    oldPassword?: string
    newPassword?: string
  }

  type UserUpdateRequest = {
    id?: number
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    userIdentity?: string
    userIndustry?: string
    phone?: string
    email?: string
  }

  type UserBindPhoneRequest = {
    phone?: string
  }

  type UserBindEmailRequest = {
    email?: string
    emailCode?: string
  }

  type UserVO = {
    id?: number
    userAccount?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    userIdentity?: string
    userIndustry?: string
    phone?: string
    email?: string
    createTime?: string
  }

  type viewAppParams = {
    appId: number
  }

  // ===== PromptTemplate Types =====

  type PromptTemplateVO = {
    id?: number
    templateName?: string
    matchIdentity?: string
    matchIndustry?: string
    description?: string
    promptContent?: string
    templateFields?: string
    sortOrder?: number
    isEnabled?: number
    userId?: number
    createTime?: string
    updateTime?: string
  }

  type PromptTemplateAddRequest = {
    templateName?: string
    matchIdentity?: string
    matchIndustry?: string
    description?: string
    promptContent?: string
    templateFields?: string
    sortOrder?: number
    isEnabled?: number
  }

  type PromptTemplateUpdateRequest = {
    id?: number
    templateName?: string
    matchIdentity?: string
    matchIndustry?: string
    description?: string
    promptContent?: string
    templateFields?: string
    sortOrder?: number
    isEnabled?: number
  }

  type PromptTemplateQueryRequest = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    id?: number
    templateName?: string
    matchIdentity?: string
    matchIndustry?: string
    isEnabled?: number
  }

  type BaseResponsePromptTemplateVO = {
    code?: number
    data?: PromptTemplateVO
    message?: string
  }

  type BaseResponseListPromptTemplateVO = {
    code?: number
    data?: PromptTemplateVO[]
    message?: string
  }

  type PagePromptTemplateVO = {
    records?: PromptTemplateVO[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
  }

  type BaseResponsePagePromptTemplateVO = {
    code?: number
    data?: PagePromptTemplateVO
    message?: string
  }

  // ===== SystemPrompt Types =====

  type SystemPromptVO = {
    id?: number
    promptName?: string
    promptKey?: string
    promptContent?: string
    description?: string
    userId?: number
    createTime?: string
    updateTime?: string
  }

  type SystemPromptAddRequest = {
    promptName?: string
    promptKey?: string
    promptContent?: string
    description?: string
  }

  type SystemPromptUpdateRequest = {
    id?: number
    promptName?: string
    promptKey?: string
    promptContent?: string
    description?: string
  }

  type SystemPromptQueryRequest = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    id?: number
    promptName?: string
    promptKey?: string
  }

  type BaseResponseSystemPromptVO = {
    code?: number
    data?: SystemPromptVO
    message?: string
  }

  type BaseResponseListSystemPromptVO = {
    code?: number
    data?: SystemPromptVO[]
    message?: string
  }

  type PageSystemPromptVO = {
    records?: SystemPromptVO[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
  }

  type BaseResponsePageSystemPromptVO = {
    code?: number
    data?: PageSystemPromptVO
    message?: string
  }

  // ===== PromptTemplate Field Definition =====

  type TemplateField = {
    type: 'select' | 'text' | 'switch'
    key: string
    label: string
    options?: string[]
    defaultValue?: any
    trueText?: string
    falseText?: string
  }

  // ===== Social Module Types =====

  type CommunityReplyVO = {
    id?: number | string
    postId?: number | string
    userId?: number | string
    content?: string
    likeCount?: number
    createTime?: string
    user?: UserVO
    hasLiked?: boolean
  }

  type CommunityPostVO = {
    id?: number | string
    title?: string
    content?: string
    category?: string
    userId?: number | string
    viewCount?: number
    likeCount?: number
    replyCount?: number
    isTop?: number
    createTime?: string
    updateTime?: string
    user?: UserVO
    hasLiked?: boolean
    latestReplies?: CommunityReplyVO[]
  }

  type CommunityPostAddRequest = {
    title?: string
    content?: string
    category?: string
  }

  type CommunityReplyAddRequest = {
    postId?: number | string
    content?: string
  }

  type CommunityPostQueryRequest = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    category?: string
    searchText?: string
  }

  type CommunityReplyQueryRequest = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    postId?: number | string
  }

  type PageCommunityPostVO = {
    records?: CommunityPostVO[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
  }

  type PageCommunityReplyVO = {
    records?: CommunityReplyVO[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
  }

  type BaseResponseCommunityPostVO = {
    code?: number
    data?: CommunityPostVO
    message?: string
  }

  type BaseResponsePageCommunityPostVO = {
    code?: number
    data?: PageCommunityPostVO
    message?: string
  }

  type BaseResponsePageCommunityReplyVO = {
    code?: number
    data?: PageCommunityReplyVO
    message?: string
  }

  type AppHotStatVO = {
    appId?: number
    likeCount?: number
    shareCount?: number
    favoriteCount?: number
    commentCount?: number
    hasLiked?: boolean
    hasFavorited?: boolean
  }

  type BaseResponseAppHotStatVO = {
    code?: number
    data?: AppHotStatVO
    message?: string
  }

  type AppCommentVO = {
    id?: number
    appId?: number
    userId?: number
    content?: string
    likeCount?: number
    createTime?: string
    user?: UserVO
    hasLiked?: boolean
  }

  type AppCommentAddRequest = {
    appId?: number
    content?: string
  }

  type AppCommentQueryRequest = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    appId?: number
  }

  type PageAppCommentVO = {
    records?: AppCommentVO[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
  }

  type BaseResponsePageAppCommentVO = {
    code?: number
    data?: PageAppCommentVO
    message?: string
  }

  type AppFavoriteQueryRequest = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
  }

  type AppFavoriteRecord = {
    id?: number
    appId?: number
    userId?: number
    createTime?: string
  }

  type PageAppFavoriteRecord = {
    records?: AppFavoriteRecord[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
  }

  type BaseResponsePageAppFavoriteRecord = {
    code?: number
    data?: PageAppFavoriteRecord
    message?: string
  }

  type AppHotStat = {
    id?: number
    appId?: number
    likeCount?: number
    shareCount?: number
    favoriteCount?: number
    commentCount?: number
    createTime?: string
    updateTime?: string
  }

  type PageAppHotStat = {
    records?: AppHotStat[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
  }

  type BaseResponsePageAppHotStat = {
    code?: number
    data?: PageAppHotStat
    message?: string
  }

  // ===== RAG Document Types =====

  type RagDocumentVO = {
    id?: number
    bizType?: string
    docTitle?: string
    docContent?: string
    codeGenType?: string
    description?: string
    isEnabled?: number
    sortOrder?: number
    userId?: number
    createTime?: string
    updateTime?: string
  }

  type RagDocumentAddRequest = {
    bizType?: string
    docTitle?: string
    docContent?: string
    codeGenType?: string
    description?: string
    isEnabled?: number
    sortOrder?: number
  }

  type RagDocumentUpdateRequest = {
    id?: number
    bizType?: string
    docTitle?: string
    docContent?: string
    codeGenType?: string
    description?: string
    isEnabled?: number
    sortOrder?: number
  }

  type RagDocumentQueryRequest = {
    pageNum?: number
    pageSize?: number
    bizType?: string
    docTitle?: string
    codeGenType?: string
    isEnabled?: number
  }

  type PageRagDocumentVO = {
    records?: RagDocumentVO[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
  }

  type BaseResponseRagDocumentVO = {
    code?: number
    data?: RagDocumentVO
    message?: string
  }

  type BaseResponsePageRagDocumentVO = {
    code?: number
    data?: PageRagDocumentVO
    message?: string
  }

  // ===== Customer Service Types =====

  type CustomerServiceConversationVO = {
    id?: number
    title?: string
    lastMessagePreview?: string
    lastMessageTime?: string
    createTime?: string
    updateTime?: string
  }

  type CustomerServiceMessageVO = {
    id?: number
    conversationId?: number
    senderType?: string
    content?: string
    createTime?: string
  }

  type BaseResponseCustomerServiceConversationVO = {
    code?: number
    data?: CustomerServiceConversationVO
    message?: string
  }

  type BaseResponseListCustomerServiceConversationVO = {
    code?: number
    data?: CustomerServiceConversationVO[]
    message?: string
  }

  type BaseResponseListCustomerServiceMessageVO = {
    code?: number
    data?: CustomerServiceMessageVO[]
    message?: string
  }

  type listCustomerServiceMessagesParams = {
    conversationId: number
    pageSize?: number
    lastCreateTime?: string
  }

  // ===== RAG Param Types =====

  type RagParamVO = {
    id?: number
    paramKey?: string
    paramName?: string
    paramValue?: string
    paramType?: string
    paramGroup?: string
    description?: string
    sortOrder?: number
    updateTime?: string
  }

  type RagParamUpdateRequest = {
    id?: number
    paramValue?: string
  }

  type BaseResponseListRagParamVO = {
    code?: number
    data?: RagParamVO[]
    message?: string
  }

  type BaseResponseBoolean = {
    code?: number
    data?: boolean
    message?: string
  }

  // ===== Material Module Types =====

  type MaterialCategoryVO = {
    id?: number
    categoryName?: string
    categoryCode?: string
    categoryIcon?: string
    description?: string
    sortOrder?: number
    isEnabled?: number
    materialCount?: number
    createTime?: string
    updateTime?: string
  }

  type MaterialCategoryAddRequest = {
    categoryName?: string
    categoryCode?: string
    categoryIcon?: string
    description?: string
    sortOrder?: number
    isEnabled?: number
  }

  type MaterialCategoryUpdateRequest = {
    id?: number
    categoryName?: string
    categoryCode?: string
    categoryIcon?: string
    description?: string
    sortOrder?: number
    isEnabled?: number
  }

  type MaterialCategoryQueryRequest = {
    pageNum?: number
    pageSize?: number
    id?: number
    categoryName?: string
    categoryCode?: string
    isEnabled?: number
  }

  type PageMaterialCategoryVO = {
    records?: MaterialCategoryVO[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
  }

  type BaseResponseMaterialCategoryVO = {
    code?: number
    data?: MaterialCategoryVO
    message?: string
  }

  type BaseResponsePageMaterialCategoryVO = {
    code?: number
    data?: PageMaterialCategoryVO
    message?: string
  }

  type BaseResponseListMaterialCategoryVO = {
    code?: number
    data?: MaterialCategoryVO[]
    message?: string
  }

  type MaterialVO = {
    id?: number
    materialName?: string
    categoryId?: number
    categoryName?: string
    materialType?: string
    content?: string
    thumbnailUrl?: string
    fileSize?: number
    description?: string
    tags?: string
    isPublic?: number
    useCount?: number
    userId?: number
    user?: UserVO
    createTime?: string
    updateTime?: string
  }

  type MaterialAddRequest = {
    materialName?: string
    categoryId?: number
    materialType?: string
    content?: string
    thumbnailUrl?: string
    fileSize?: number
    description?: string
    tags?: string
    isPublic?: number
  }

  type MaterialUpdateRequest = {
    id?: number
    materialName?: string
    categoryId?: number
    materialType?: string
    content?: string
    thumbnailUrl?: string
    fileSize?: number
    description?: string
    tags?: string
    isPublic?: number
  }

  type MaterialQueryRequest = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    id?: number
    materialName?: string
    categoryId?: number
    materialType?: string
    tags?: string
    isPublic?: number
    userId?: number
    searchText?: string
  }

  type PageMaterialVO = {
    records?: MaterialVO[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
  }

  type BaseResponseMaterialVO = {
    code?: number
    data?: MaterialVO
    message?: string
  }

  type BaseResponsePageMaterialVO = {
    code?: number
    data?: PageMaterialVO
    message?: string
  }

  type BaseResponseListMaterialVO = {
    code?: number
    data?: MaterialVO[]
    message?: string
  }

  // ===== User Friendship Types =====

  type UserFriendshipVO = {
    id?: number
    userId?: number
    friendId?: number
    status?: number
    remark?: string
    friendName?: string
    friendAvatar?: string
    createTime?: string
    updateTime?: string
  }

  type FriendAddRequest = {
    friendId?: number
    remark?: string
  }

  type FriendHandleRequest = {
    id?: number
    action?: number
  }

  type BaseResponseListUserFriendshipVO = {
    code?: number
    data?: UserFriendshipVO[]
    message?: string
  }

  type BaseResponseListUserVO = {
    code?: number
    data?: UserVO[]
    message?: string
  }

  // ===== User Chat Types =====

  type ChatConversationVO = {
    id?: number
    targetUserId?: number
    targetUserName?: string
    targetUserAvatar?: string
    lastMessageContent?: string
    lastMessageType?: string
    lastMessageTime?: string
    unreadCount?: number
    createTime?: string
  }

  type ChatMessageVO = {
    id?: number
    conversationId?: number
    senderId?: number
    senderName?: string
    senderAvatar?: string
    receiverId?: number
    content?: string
    messageType?: string
    collabId?: number
    appId?: number
    appName?: string
    appCover?: string
    collabRole?: string
    collabStatus?: number
    isRead?: number
    createTime?: string
  }

  type ChatMessageSendRequest = {
    receiverId?: number
    content?: string
    messageType?: string
  }

  type ChatMessageQueryRequest = {
    pageNum?: number
    pageSize?: number
    conversationId?: number
  }

  type PageChatMessageVO = {
    records?: ChatMessageVO[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
  }

  type BaseResponseListChatConversationVO = {
    code?: number
    data?: ChatConversationVO[]
    message?: string
  }

  type BaseResponsePageChatMessageVO = {
    code?: number
    data?: PageChatMessageVO
    message?: string
  }

  type BaseResponseChatMessageVO = {
    code?: number
    data?: ChatMessageVO
    message?: string
  }

  type BaseResponseInteger = {
    code?: number
    data?: number
    message?: string
  }

  // ===== App Collaborator Types =====

  /** 产物协作者视图对象 */
  type AppCollaboratorVO = {
    id?: number
    appId?: number
    appName?: string
    appCover?: string
    userId?: number
    userName?: string
    userAvatar?: string
    inviterId?: number
    inviterName?: string
    /** 状态: 0=待确认, 1=已接受, 2=已拒绝, 3=已移除 */
    status?: number
    /** 协作角色: editor(编辑者), viewer(查看者) */
    role?: string
    createTime?: string
    updateTime?: string
  }

  /** 协作者邀请请求 */
  type CollaboratorInviteRequest = {
    appId?: number
    userId?: number
    role?: string
  }

  /** 协作邀请处理请求 */
  type CollaboratorHandleRequest = {
    id?: number
    /** 1=接受, 2=拒绝 */
    action?: number
  }

  type BaseResponseListAppCollaboratorVO = {
    code?: number
    data?: AppCollaboratorVO[]
    message?: string
  }
}
